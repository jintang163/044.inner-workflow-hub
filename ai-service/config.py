import os

GRPC_PORT = int(os.getenv('GRPC_PORT', 50051))
MODEL_PATH = os.getenv('MODEL_PATH', './models')
MODEL_VERSION = os.getenv('MODEL_VERSION', '1.0.0')
TRAIN_WINDOW_DAYS = int(os.getenv('TRAIN_WINDOW_DAYS', 180))
APPROVE_THRESHOLD = float(os.getenv('APPROVE_THRESHOLD', 0.7))

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': int(os.getenv('DB_PORT', 3306)),
    'user': os.getenv('DB_USER', 'root'),
    'password': os.getenv('DB_PASSWORD', ''),
    'database': os.getenv('DB_NAME', 'workflow'),
    'charset': 'utf8mb4'
}

LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
