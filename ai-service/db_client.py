import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional

from config import DB_CONFIG, TRAIN_WINDOW_DAYS

logger = logging.getLogger(__name__)


class DBClient:
    def __init__(self):
        self.connection = None
        self._connect()

    def _connect(self) -> bool:
        try:
            import pymysql
            self.connection = pymysql.connect(**DB_CONFIG)
            logger.info("Database connection established")
            return True
        except Exception as e:
            logger.warning(f"Failed to connect to database: {e}")
            self.connection = None
            return False

    def _ensure_connection(self) -> bool:
        if self.connection is None:
            return self._connect()
        try:
            self.connection.ping(reconnect=True)
            return True
        except Exception as e:
            logger.warning(f"Database connection lost, reconnecting: {e}")
            return self._connect()

    def fetch_historical_approvals(self, days: Optional[int] = None) -> List[Dict]:
        days = days or TRAIN_WINDOW_DAYS

        if not self._ensure_connection():
            logger.warning("No database connection, returning empty list")
            return []

        try:
            with self.connection.cursor() as cursor:
                query = """
                    SELECT
                        h.id,
                        h.instance_id,
                        h.amount,
                        h.department_id,
                        h.initiator_id,
                        h.initiator_level,
                        h.approver_id,
                        h.process_key,
                        h.business_line_id,
                        h.priority,
                        h.form_data_json,
                        h.approved,
                        h.created_at
                    FROM wf_approval_history h
                    WHERE h.created_at >= %s
                    AND h.approved IS NOT NULL
                    ORDER BY h.created_at DESC
                """

                start_date = datetime.now() - timedelta(days=days)
                cursor.execute(query, (start_date,))

                columns = [desc[0] for desc in cursor.description]
                results = []

                for row in cursor.fetchall():
                    item = dict(zip(columns, row))

                    if 'created_at' in item and item['created_at']:
                        item['created_at'] = item['created_at'].isoformat() if hasattr(item['created_at'], 'isoformat') else str(item['created_at'])

                    results.append(item)

                logger.info(f"Fetched {len(results)} historical records from DB")
                return results

        except Exception as e:
            logger.error(f"Failed to fetch historical approvals: {e}")
            return []

    def fetch_approval_history_by_approver(self, approver_id: int, days: Optional[int] = None) -> List[Dict]:
        days = days or TRAIN_WINDOW_DAYS

        if not self._ensure_connection():
            return []

        try:
            with self.connection.cursor() as cursor:
                query = """
                    SELECT
                        h.id,
                        h.instance_id,
                        h.amount,
                        h.department_id,
                        h.department_rate,
                        h.initiator_id,
                        h.initiator_level,
                        h.approver_id,
                        h.process_key,
                        h.business_line_id,
                        h.priority,
                        h.form_data_json,
                        h.approved,
                        h.created_at
                    FROM wf_approval_history h
                    WHERE h.approver_id = %s
                    AND h.created_at >= %s
                    AND h.approved IS NOT NULL
                    ORDER BY h.created_at DESC
                """

                start_date = datetime.now() - timedelta(days=days)
                cursor.execute(query, (approver_id, start_date))

                columns = [desc[0] for desc in cursor.description]
                results = [dict(zip(columns, row)) for row in cursor.fetchall()]

                logger.info(f"Fetched {len(results)} records for approver {approver_id}")
                return results

        except Exception as e:
            logger.error(f"Failed to fetch approver history: {e}")
            return []

    def fetch_approval_history_by_initiator(self, initiator_id: int, days: Optional[int] = None) -> List[Dict]:
        days = days or TRAIN_WINDOW_DAYS

        if not self._ensure_connection():
            return []

        try:
            with self.connection.cursor() as cursor:
                query = """
                    SELECT
                        h.id,
                        h.instance_id,
                        h.amount,
                        h.department_id,
                        h.initiator_id,
                        h.initiator_level,
                        h.approver_id,
                        h.process_key,
                        h.business_line_id,
                        h.priority,
                        h.form_data_json,
                        h.approved,
                        h.created_at
                    FROM wf_approval_history h
                    WHERE h.initiator_id = %s
                    AND h.created_at >= %s
                    AND h.approved IS NOT NULL
                    ORDER BY h.created_at DESC
                """

                start_date = datetime.now() - timedelta(days=days)
                cursor.execute(query, (initiator_id, start_date))

                columns = [desc[0] for desc in cursor.description]
                results = [dict(zip(columns, row)) for row in cursor.fetchall()]

                logger.info(f"Fetched {len(results)} records for initiator {initiator_id}")
                return results

        except Exception as e:
            logger.error(f"Failed to fetch initiator history: {e}")
            return []

    def close(self):
        if self.connection:
            try:
                self.connection.close()
                logger.info("Database connection closed")
            except Exception as e:
                logger.error(f"Error closing connection: {e}")
            finally:
                self.connection = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


def get_db_client() -> DBClient:
    return DBClient()
