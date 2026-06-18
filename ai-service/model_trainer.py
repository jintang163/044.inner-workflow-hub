import logging
import os
import joblib
import numpy as np
import pandas as pd
from typing import Dict, List, Optional, Tuple, Any
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score

from feature_engineering import FEATURE_COLUMNS, extract_features

logger = logging.getLogger(__name__)


def train_model(training_data: List[Dict], model_type: str = 'lightgbm') -> Tuple[Any, Dict]:
    logger.info(f"Training {model_type} model with {len(training_data)} samples")

    df = _prepare_training_data(training_data)

    if len(df) < 10:
        logger.warning("Insufficient training data, generating synthetic data")
        df = _generate_synthetic_data(200)

    X = df[FEATURE_COLUMNS].values
    y = df['approved'].values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model = None
    metrics = {}

    try:
        if model_type == 'lightgbm':
            model = _train_lightgbm(X_train, y_train)
        elif model_type == 'xgboost':
            model = _train_xgboost(X_train, y_train)
        else:
            logger.warning(f"Unknown model type {model_type}, falling back to lightgbm")
            model = _train_lightgbm(X_train, y_train)

        y_pred = model.predict(X_test)
        if hasattr(model, 'predict_proba'):
            y_pred_proba = model.predict_proba(X_test)[:, 1]
            y_pred = (y_pred_proba >= 0.5).astype(int)

        accuracy = accuracy_score(y_test, y_pred)
        metrics['accuracy'] = float(accuracy)
        metrics['train_samples'] = len(X_train)
        metrics['test_samples'] = len(X_test)

        logger.info(f"Model training complete. Accuracy: {accuracy:.4f}")

    except Exception as e:
        logger.error(f"Failed to train {model_type}: {e}, falling back to xgboost")
        try:
            model = _train_xgboost(X_train, y_train)
            y_pred = model.predict(X_test)
            accuracy = accuracy_score(y_test, y_pred)
            metrics['accuracy'] = float(accuracy)
            logger.info(f"XGBoost fallback training complete. Accuracy: {accuracy:.4f}")
        except Exception as e2:
            logger.error(f"Both models failed: {e2}")
            raise

    return model, metrics


def _prepare_training_data(training_data: List[Dict]) -> pd.DataFrame:
    processed_items = []

    for item in training_data:
        features = extract_features(
            instance_id=item.get('instance_id', ''),
            amount=item.get('amount', 0.0),
            department_id=item.get('department_id', 0),
            initiator_id=item.get('initiator_id', 0),
            initiator_level=item.get('initiator_level', 0),
            approver_id=item.get('approver_id', 0),
            process_key=item.get('process_key', 'default'),
            business_line_id=item.get('business_line_id', 0),
            priority=item.get('priority', 0),
            form_data_json=item.get('form_data_json', ''),
            historical_data=training_data
        )
        features['approved'] = int(item.get('approved', False))
        processed_items.append(features)

    return pd.DataFrame(processed_items)


def _generate_synthetic_data(n_samples: int = 200) -> pd.DataFrame:
    np.random.seed(42)

    data = []
    for i in range(n_samples):
        amount = np.exp(np.random.uniform(0, 12))
        department_id = np.random.randint(1, 20)
        initiator_id = np.random.randint(1, 100)
        initiator_level = np.random.randint(1, 9)
        approver_id = np.random.randint(1, 50)
        process_keys = ['leave', 'expense', 'purchase', 'contract', 'recruitment']
        process_key = np.random.choice(process_keys)
        business_line_id = np.random.randint(1, 10)
        priority = np.random.randint(0, 4)

        amount_level = 1 if amount < 1000 else 2 if amount < 5000 else 3 if amount < 10000 else 4 if amount < 50000 else 5 if amount < 100000 else 6

        base_prob = 0.75
        if amount_level >= 5:
            base_prob -= 0.2
        if priority >= 2:
            base_prob += 0.1
        if initiator_level <= 3:
            base_prob += 0.1

        approved = np.random.random() < max(0.2, min(0.95, base_prob))

        features = extract_features(
            instance_id=f'syn_{i}',
            amount=amount,
            department_id=department_id,
            initiator_id=initiator_id,
            initiator_level=initiator_level,
            approver_id=approver_id,
            process_key=process_key,
            business_line_id=business_line_id,
            priority=priority,
            form_data_json='{}',
            historical_data=None
        )
        features['approved'] = int(approved)
        data.append(features)

    return pd.DataFrame(data)


def _train_lightgbm(X_train: np.ndarray, y_train: np.ndarray):
    import lightgbm as lgb

    params = {
        'objective': 'binary',
        'metric': 'binary_logloss',
        'boosting_type': 'gbdt',
        'num_leaves': 31,
        'learning_rate': 0.05,
        'feature_fraction': 0.9,
        'bagging_fraction': 0.8,
        'bagging_freq': 5,
        'verbose': -1,
        'min_data_in_leaf': 10,
        'max_depth': 6
    }

    lgb_train = lgb.Dataset(X_train, label=y_train)
    model = lgb.train(
        params,
        lgb_train,
        num_boost_round=200,
        valid_sets=[lgb_train],
        callbacks=[lgb.log_evaluation(period=0)]
    )

    return model


def _train_xgboost(X_train: np.ndarray, y_train: np.ndarray):
    import xgboost as xgb

    params = {
        'objective': 'binary:logistic',
        'eval_metric': 'logloss',
        'max_depth': 6,
        'learning_rate': 0.05,
        'subsample': 0.8,
        'colsample_bytree': 0.9,
        'min_child_weight': 3,
        'verbosity': 0
    }

    dtrain = xgb.DMatrix(X_train, label=y_train)
    model = xgb.train(params, dtrain, num_boost_round=200)

    return model


def save_model(model: Any, path: str, model_version: str = '1.0.0') -> str:
    os.makedirs(os.path.dirname(path), exist_ok=True)

    model_path = f"{path}/approval_model_{model_version}.joblib"
    joblib.dump(model, model_path)

    logger.info(f"Model saved to {model_path}")
    return model_path


def load_model(path: str) -> Optional[Any]:
    if not os.path.exists(path):
        logger.warning(f"Model file not found: {path}")
        return None

    try:
        model = joblib.load(path)
        logger.info(f"Model loaded from {path}")
        return model
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        return None


def incremental_train(model: Any, new_data: List[Dict]) -> Tuple[Any, Dict]:
    logger.info(f"Incremental training with {len(new_data)} new samples")

    df = _prepare_training_data(new_data)

    X = df[FEATURE_COLUMNS].values
    y = df['approved'].values

    try:
        if hasattr(model, 'refit') or 'lightgbm' in str(type(model)).lower():
            import lightgbm as lgb
            lgb_train = lgb.Dataset(X, label=y)
            params = model.params if hasattr(model, 'params') else {
                'objective': 'binary',
                'metric': 'binary_logloss',
                'verbose': -1
            }
            updated_model = lgb.train(params, lgb_train, num_boost_round=50, init_model=model)
        else:
            updated_model, metrics = train_model(new_data, model_type='xgboost')

        y_pred = updated_model.predict(X)
        if hasattr(updated_model, 'predict_proba'):
            y_pred_proba = updated_model.predict_proba(X)[:, 1]
            y_pred = (y_pred_proba >= 0.5).astype(int)

        accuracy = accuracy_score(y, y_pred)
        metrics = {'accuracy': float(accuracy), 'samples': len(X)}

        logger.info(f"Incremental training complete. Accuracy: {accuracy:.4f}")
        return updated_model, metrics

    except Exception as e:
        logger.error(f"Incremental training failed, training new model: {e}")
        return train_model(new_data)


def feature_importance(model: Any) -> Dict[str, float]:
    importance = {}

    try:
        if hasattr(model, 'feature_importance'):
            importances = model.feature_importance()
            for i, col in enumerate(FEATURE_COLUMNS):
                if i < len(importances):
                    importance[col] = float(importances[i])
        elif hasattr(model, 'get_score'):
            scores = model.get_score()
            for i, col in enumerate(FEATURE_COLUMNS):
                importance[col] = float(scores.get(f'f{i}', 0.0))
        elif hasattr(model, 'feature_importances_'):
            importances = model.feature_importances_
            for i, col in enumerate(FEATURE_COLUMNS):
                if i < len(importances):
                    importance[col] = float(importances[i])
        else:
            logger.warning("Could not extract feature importance")
            for col in FEATURE_COLUMNS:
                importance[col] = 1.0 / len(FEATURE_COLUMNS)
    except Exception as e:
        logger.error(f"Failed to get feature importance: {e}")
        for col in FEATURE_COLUMNS:
            importance[col] = 1.0 / len(FEATURE_COLUMNS)

    total = sum(importance.values())
    if total > 0:
        importance = {k: v / total for k, v in importance.items()}

    return importance


def predict(model: Any, X: np.ndarray) -> Tuple[float, int]:
    try:
        if hasattr(model, 'predict'):
            pred = model.predict(X)

            if isinstance(pred, np.ndarray) and pred.ndim > 1:
                proba = float(pred[0, 1]) if pred.shape[1] > 1 else float(pred[0, 0])
            else:
                proba = float(pred[0]) if len(pred) > 0 else 0.5

            if proba < 0 or proba > 1:
                proba = 1.0 / (1.0 + np.exp(-proba))

            prediction = 1 if proba >= 0.5 else 0
            return proba, prediction

    except Exception as e:
        logger.error(f"Prediction failed: {e}")

    return 0.5, 0
