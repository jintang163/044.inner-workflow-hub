import json
import logging
import numpy as np
from typing import Dict, List, Optional

logger = logging.getLogger(__name__)

FEATURE_COLUMNS = [
    'amount', 'amount_level', 'department_id', 'department_rate',
    'initiator_id', 'initiator_level', 'initiator_level_rate',
    'approver_id', 'approver_approval_rate', 'process_type',
    'business_line_id', 'priority'
]

PROCESS_KEY_MAPPING = {
    'leave': 1,
    'expense': 2,
    'purchase': 3,
    'contract': 4,
    'recruitment': 5,
    'promotion': 6,
    'resignation': 7,
    'default': 0
}


def extract_features(
    instance_id: str,
    amount: float,
    department_id: int,
    initiator_id: int,
    initiator_level: int,
    approver_id: int,
    process_key: str,
    business_line_id: int,
    priority: int,
    form_data_json: str,
    historical_data: Optional[List[Dict]] = None,
    department_rate: float = 0.75,
    initiator_level_rate: float = 0.75,
    approver_approval_rate: float = 0.75,
    initiator_approval_rate: float = 0.75
) -> Dict:
    historical_data = historical_data or []

    amount_level = _get_amount_level(amount)
    dept_rate = department_rate if department_rate and department_rate > 0 else compute_department_rate(department_id, historical_data)
    init_level_rate = initiator_level_rate if initiator_level_rate and initiator_level_rate > 0 else _get_initiator_level_rate(initiator_level)
    approver_rate = approver_approval_rate if approver_approval_rate and approver_approval_rate > 0 else compute_approver_rate(approver_id, historical_data)
    process_type = _get_process_type(process_key)
    init_rate = initiator_approval_rate if initiator_approval_rate and initiator_approval_rate > 0 else compute_initiator_rate(initiator_id, historical_data)

    features = {
        'instance_id': instance_id,
        'amount': amount,
        'amount_level': amount_level,
        'department_id': department_id,
        'department_rate': dept_rate,
        'initiator_id': initiator_id,
        'initiator_level': initiator_level,
        'initiator_level_rate': init_level_rate,
        'approver_id': approver_id,
        'approver_approval_rate': approver_rate,
        'process_type': process_type,
        'business_line_id': business_line_id,
        'priority': priority,
        'initiator_approval_rate': init_rate
    }

    if form_data_json:
        try:
            form_data = json.loads(form_data_json)
            features['form_field_count'] = len(form_data) if isinstance(form_data, dict) else 0
        except (json.JSONDecodeError, TypeError) as e:
            logger.warning(f"Failed to parse form_data_json: {e}")
            features['form_field_count'] = 0
    else:
        features['form_field_count'] = 0

    return features


def _get_amount_level(amount: float) -> int:
    if amount < 1000:
        return 1
    elif amount < 5000:
        return 2
    elif amount < 10000:
        return 3
    elif amount < 50000:
        return 4
    elif amount < 100000:
        return 5
    else:
        return 6


def _get_initiator_level_rate(level: int) -> float:
    level_rates = {
        1: 0.95,
        2: 0.90,
        3: 0.85,
        4: 0.80,
        5: 0.75,
        6: 0.70,
        7: 0.65,
        8: 0.60
    }
    return level_rates.get(level, 0.75)


def _get_process_type(process_key: str) -> int:
    return PROCESS_KEY_MAPPING.get(process_key, PROCESS_KEY_MAPPING['default'])


def compute_approver_rate(approver_id: int, historical_data: List[Dict]) -> float:
    if not historical_data:
        return 0.75

    approver_approvals = [
        item for item in historical_data
        if item.get('approver_id') == approver_id
    ]

    if not approver_approvals:
        return 0.75

    approved_count = sum(1 for item in approver_approvals if item.get('approved', False))
    return approved_count / len(approver_approvals)


def compute_initiator_rate(initiator_id: int, historical_data: List[Dict]) -> float:
    if not historical_data:
        return 0.75

    initiator_approvals = [
        item for item in historical_data
        if item.get('initiator_id') == initiator_id
    ]

    if not initiator_approvals:
        return 0.75

    approved_count = sum(1 for item in initiator_approvals if item.get('approved', False))
    return approved_count / len(initiator_approvals)


def compute_department_rate(department_id: int, historical_data: List[Dict]) -> float:
    if not historical_data:
        return 0.75

    dept_approvals = [
        item for item in historical_data
        if item.get('department_id') == department_id
    ]

    if not dept_approvals:
        return 0.75

    approved_count = sum(1 for item in dept_approvals if item.get('approved', False))
    return approved_count / len(dept_approvals)


def generate_factor_explanations(feature_values: Dict, model_importance: Optional[Dict] = None) -> List[Dict]:
    default_weights = {
        'amount_level': 0.20,
        'department_rate': 0.15,
        'initiator_level_rate': 0.15,
        'approver_approval_rate': 0.25,
        'process_type': 0.10,
        'priority': 0.10,
        'business_line_id': 0.05
    }

    weights = model_importance or default_weights

    amount_level = feature_values.get('amount_level', 0)
    amount_level_desc = _describe_amount_level(amount_level)

    dept_rate = feature_values.get('department_rate', 0.75)
    initiator_rate = feature_values.get('initiator_level_rate', 0.75)
    approver_rate = feature_values.get('approver_approval_rate', 0.75)
    process_type = feature_values.get('process_type', 0)
    priority = feature_values.get('priority', 0)
    business_line = feature_values.get('business_line_id', 0)

    factors = [
        {
            'key': 'amount_level',
            'value': amount_level_desc,
            'weight': weights.get('amount_level', default_weights['amount_level'])
        },
        {
            'key': 'department_rate',
            'value': f'{dept_rate:.1%}',
            'weight': weights.get('department_rate', default_weights['department_rate'])
        },
        {
            'key': 'initiator_level_rate',
            'value': f'{initiator_rate:.1%}',
            'weight': weights.get('initiator_level_rate', default_weights['initiator_level_rate'])
        },
        {
            'key': 'approver_approval_rate',
            'value': f'{approver_rate:.1%}',
            'weight': weights.get('approver_approval_rate', default_weights['approver_approval_rate'])
        },
        {
            'key': 'process_type',
            'value': _describe_process_type(process_type),
            'weight': weights.get('process_type', default_weights['process_type'])
        },
        {
            'key': 'priority',
            'value': _describe_priority(priority),
            'weight': weights.get('priority', default_weights['priority'])
        },
        {
            'key': 'business_line',
            'value': f'Line {business_line}',
            'weight': weights.get('business_line_id', default_weights['business_line_id'])
        }
    ]

    factors.sort(key=lambda x: x['weight'], reverse=True)
    return factors


def _describe_amount_level(level: int) -> str:
    descriptions = {
        1: 'Very Low (<1K)',
        2: 'Low (1K-5K)',
        3: 'Medium (5K-10K)',
        4: 'High (10K-50K)',
        5: 'Very High (50K-100K)',
        6: 'Critical (>100K)'
    }
    return descriptions.get(level, 'Unknown')


def _describe_process_type(process_type: int) -> str:
    reverse_mapping = {v: k for k, v in PROCESS_KEY_MAPPING.items()}
    return reverse_mapping.get(process_type, 'unknown').title()


def _describe_priority(priority: int) -> str:
    descriptions = {
        0: 'Low',
        1: 'Medium',
        2: 'High',
        3: 'Urgent'
    }
    return descriptions.get(priority, f'Level {priority}')


def features_to_array(feature_dict: Dict) -> np.ndarray:
    feature_list = []
    for col in FEATURE_COLUMNS:
        feature_list.append(float(feature_dict.get(col, 0.0)))
    return np.array(feature_list).reshape(1, -1)
