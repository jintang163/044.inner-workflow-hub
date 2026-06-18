import logging
import os
import sys
import time
import random
from datetime import datetime
from typing import Dict, List, Optional
from concurrent import futures

import grpc
from google.protobuf import empty_pb2

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'proto'))

from config import GRPC_PORT, MODEL_PATH, MODEL_VERSION, APPROVE_THRESHOLD, LOG_LEVEL
from feature_engineering import extract_features, generate_factor_explanations, features_to_array
from model_trainer import train_model, save_model, load_model, incremental_train, feature_importance, predict

try:
    from proto import approval_ai_pb2 as pb2
    from proto import approval_ai_pb2_grpc as pb2_grpc
except ImportError:
    print("WARNING: gRPC code not generated. Run: python -m grpc_tools.protoc -I./proto --python_out=./proto --grpc_python_out=./proto ./proto/approval_ai.proto")
    import approval_ai_pb2 as pb2
    import approval_ai_pb2_grpc as pb2_grpc

logging.basicConfig(
    level=getattr(logging, LOG_LEVEL, logging.INFO),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class ApprovalAiService(pb2_grpc.ApprovalAiServiceServicer):
    def __init__(self):
        self.model = None
        self.model_version = MODEL_VERSION
        self.total_trained_samples = 0
        self.last_training_time = ""
        self.accuracy = 0.0
        self.feature_importance_map: Dict[str, float] = {}
        self.historical_data: List[Dict] = []
        self._lock = False

        self._initialize_model()

    def _generate_synthetic_data(self, sample_count=2000) -> List[Dict]:
        logger.info(f"Generating {sample_count} synthetic training samples...")
        random.seed(42)
        samples = []
        process_keys = ['leave', 'expense', 'purchase', 'contract', 'recruitment', 'default']
        for i in range(sample_count):
            amount = random.choice([500, 1500, 4000, 8000, 20000, 60000, 150000])
            amount_level = 1 if amount < 1000 else 2 if amount < 5000 else 3 if amount < 10000 else 4 if amount < 50000 else 5 if amount < 100000 else 6
            initiator_level = random.randint(1, 8)
            initiator_level_rate = max(0.6, min(0.95, 0.98 - initiator_level * 0.04))
            department_rate = random.uniform(0.65, 0.95)
            approver_approval_rate = random.uniform(0.65, 0.95)
            initiator_approval_rate = random.uniform(0.65, 0.95)
            priority = random.choice([1, 2, 3])
            process_key = random.choice(process_keys)
            process_type_mapping = {k: v for v, k in enumerate(['leave', 'expense', 'purchase', 'contract', 'recruitment', 'promotion', 'resignation', 'default'])}
            process_type = process_type_mapping.get(process_key, 0)

            approve_score = (
                (1.0 - amount_level * 0.06) +
                department_rate * 0.3 +
                initiator_level_rate * 0.25 +
                approver_approval_rate * 0.25 +
                initiator_approval_rate * 0.15 +
                (1.0 if process_type in [0, 1] else 0.0) * 0.1
            )
            approved = (approve_score + random.uniform(-0.3, 0.3)) > 1.15

            samples.append({
                'instance_id': f'syn_{i}',
                'amount': float(amount),
                'department_id': random.randint(1, 20),
                'initiator_id': random.randint(100, 500),
                'initiator_level': initiator_level,
                'approver_id': random.randint(10, 99),
                'process_key': process_key,
                'business_line_id': random.randint(1, 5),
                'priority': priority,
                'form_data_json': '',
                'approved': bool(approved),
                'amount_level': amount_level,
                'department_rate': department_rate,
                'initiator_level_rate': initiator_level_rate,
                'approver_approval_rate': approver_approval_rate,
                'initiator_approval_rate': initiator_approval_rate,
                'process_type': process_type
            })
        logger.info(f"Generated {len(samples)} synthetic samples")
        return samples

    def _initialize_model(self):
        logger.info("Initializing AI model...")

        model_file = os.path.join(MODEL_PATH, f'approval_model_{self.model_version}.joblib')

        if os.path.exists(model_file):
            logger.info(f"Loading existing model from {model_file}")
            self.model = load_model(model_file)
            if self.model:
                self._load_stats()
                self.feature_importance_map = feature_importance(self.model)
                logger.info("Model loaded successfully")
                return

        logger.info("No existing model found, training initial model with synthetic data")

        initial_training_data = self._generate_synthetic_data(2000)

        try:
            self.model, metrics = train_model(initial_training_data, model_type='lightgbm')
            self.accuracy = metrics.get('accuracy', 0.0)
            self.total_trained_samples = metrics.get('train_samples', 0) + metrics.get('test_samples', 0)
            self.last_training_time = datetime.now().isoformat()
            self.feature_importance_map = feature_importance(self.model)
            self.historical_data = initial_training_data

            save_model(self.model, MODEL_PATH, self.model_version)
            self._save_stats()

            logger.info(f"Initial model trained. Accuracy: {self.accuracy:.4f}, Samples: {self.total_trained_samples}")

        except Exception as e:
            logger.error(f"Failed to train initial LightGBM model: {e}")
            logger.info("Falling back to XGBoost...")
            try:
                self.model, metrics = train_model(initial_training_data, model_type='xgboost')
                self.accuracy = metrics.get('accuracy', 0.0)
                self.total_trained_samples = metrics.get('train_samples', 0) + metrics.get('test_samples', 0)
                self.last_training_time = datetime.now().isoformat()
                self.feature_importance_map = feature_importance(self.model)
                self.historical_data = initial_training_data

                save_model(self.model, MODEL_PATH, self.model_version)
                self._save_stats()

                logger.info(f"XGBoost fallback model trained. Accuracy: {self.accuracy:.4f}")
            except Exception as e2:
                logger.error(f"Both models failed: {e2}")
                raise

    def _save_stats(self):
        try:
            stats_file = os.path.join(MODEL_PATH, 'model_stats.txt')
            with open(stats_file, 'w') as f:
                f.write(f"total_trained_samples={self.total_trained_samples}\n")
                f.write(f"last_training_time={self.last_training_time}\n")
                f.write(f"model_version={self.model_version}\n")
                f.write(f"accuracy={self.accuracy}\n")
        except Exception as e:
            logger.warning(f"Failed to save stats: {e}")

    def _load_stats(self):
        try:
            stats_file = os.path.join(MODEL_PATH, 'model_stats.txt')
            if os.path.exists(stats_file):
                with open(stats_file, 'r') as f:
                    for line in f:
                        key, value = line.strip().split('=', 1)
                        if key == 'total_trained_samples':
                            self.total_trained_samples = int(value)
                        elif key == 'last_training_time':
                            self.last_training_time = value
                        elif key == 'model_version':
                            self.model_version = value
                        elif key == 'accuracy':
                            self.accuracy = float(value)
                logger.info("Model stats loaded")
        except Exception as e:
            logger.warning(f"Failed to load stats: {e}")

    def Predict(self, request, context):
        start_time = time.time()

        logger.info(f"Predict request received for instance: {request.instance_id}")

        try:
            feature_dict = extract_features(
                instance_id=request.instance_id,
                amount=request.amount,
                department_id=request.department_id,
                initiator_id=request.initiator_id,
                initiator_level=request.initiator_level,
                approver_id=request.approver_id,
                process_key=request.process_key,
                business_line_id=request.business_line_id,
                priority=request.priority,
                form_data_json=request.form_data_json,
                historical_data=self.historical_data,
                department_rate=request.department_rate,
                initiator_level_rate=request.initiator_level_rate,
                approver_approval_rate=request.approver_approval_rate,
                initiator_approval_rate=request.initiator_approval_rate
            )

            X = features_to_array(feature_dict)
            proba, prediction = predict(self.model, X)

            recommended_action = 1 if proba >= APPROVE_THRESHOLD else 0

            factors = generate_factor_explanations(feature_dict, self.feature_importance_map)

            if recommended_action == 1:
                reason = f"Approval recommended with {proba:.1%} confidence. "
                positive_factors = [f['key'] for f in factors[:3]]
                reason += f"Key factors: {', '.join(positive_factors)}"
            else:
                reason = f"Rejection recommended with {1-proba:.1%} confidence. "
                negative_factors = [f['key'] for f in factors if f['weight'] > 0.1][:3]
                if negative_factors:
                    reason += f"Risk factors: {', '.join(negative_factors)}"
                else:
                    reason += "Insufficient approval indicators"

            inference_ms = int((time.time() - start_time) * 1000)

            pb_factors = []
            for f in factors:
                pb_factors.append(pb2.Factor(
                    key=f['key'],
                    value=f['value'],
                    weight=f['weight']
                ))

            response = pb2.ApprovalRecommendation(
                approve_probability=float(proba),
                recommended_action=recommended_action,
                reason=reason,
                factors=pb_factors,
                model_version=self.model_version,
                inference_ms=inference_ms
            )

            logger.info(f"Prediction complete: action={recommended_action}, proba={proba:.4f}, ms={inference_ms}")
            return response

        except Exception as e:
            logger.error(f"Prediction failed: {e}")
            inference_ms = int((time.time() - start_time) * 1000)
            return pb2.ApprovalRecommendation(
                approve_probability=0.5,
                recommended_action=0,
                reason=f"Prediction error: {str(e)}",
                factors=[],
                model_version=self.model_version,
                inference_ms=inference_ms
            )

    def Train(self, request_iterator, context):
        logger.info("Training request received")

        if self._lock:
            context.set_code(grpc.StatusCode.RESOURCE_EXHAUSTED)
            context.set_details("Another training is in progress")
            return empty_pb2.Empty()

        self._lock = True
        try:
            training_data = []
            for item in request_iterator:
                training_data.append({
                    'instance_id': item.instance_id,
                    'amount': item.amount,
                    'department_id': item.department_id,
                    'initiator_id': item.initiator_id,
                    'initiator_level': item.initiator_level,
                    'approver_id': item.approver_id,
                    'process_key': item.process_key,
                    'business_line_id': item.business_line_id,
                    'priority': item.priority,
                    'form_data_json': item.form_data_json,
                    'approved': item.approved,
                    'department_rate': item.department_rate,
                    'initiator_level_rate': item.initiator_level_rate,
                    'approver_approval_rate': item.approver_approval_rate,
                    'initiator_approval_rate': item.initiator_approval_rate
                })

            logger.info(f"Received {len(training_data)} training items")

            if self.model is None:
                self.model, metrics = train_model(training_data, model_type='lightgbm')
            else:
                self.model, metrics = incremental_train(self.model, training_data)

            self.accuracy = metrics.get('accuracy', self.accuracy)
            self.total_trained_samples += len(training_data)
            self.last_training_time = datetime.now().isoformat()
            self.feature_importance_map = feature_importance(self.model)

            self.historical_data.extend(training_data)

            save_model(self.model, MODEL_PATH, self.model_version)
            self._save_stats()

            logger.info(f"Training complete. Total samples: {self.total_trained_samples}, Accuracy: {self.accuracy:.4f}")
            return empty_pb2.Empty()

        except Exception as e:
            logger.error(f"Training failed: {e}")
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Training failed: {str(e)}")
            return empty_pb2.Empty()
        finally:
            self._lock = False

    def GetStats(self, request, context):
        logger.info("GetStats request received")

        response = pb2.GetStatsResponse(
            total_trained_samples=self.total_trained_samples,
            last_training_time=self.last_training_time,
            current_model_version=self.model_version,
            accuracy=float(self.accuracy),
            feature_importance=self.feature_importance_map
        )

        return response


def serve():
    logger.info(f"Starting gRPC server on port {GRPC_PORT}...")

    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    service = ApprovalAiService()
    pb2_grpc.add_ApprovalAiServiceServicer_to_server(service, server)

    server.add_insecure_port(f'[::]:{GRPC_PORT}')
    server.start()

    logger.info(f"gRPC server started successfully on port {GRPC_PORT}")
    logger.info(f"Model version: {service.model_version}")
    logger.info(f"Total trained samples: {service.total_trained_samples}")
    logger.info(f"Model accuracy: {service.accuracy:.4f}")

    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("Server shutdown requested")
        server.stop(0)
        logger.info("Server stopped")


if __name__ == '__main__':
    serve()
