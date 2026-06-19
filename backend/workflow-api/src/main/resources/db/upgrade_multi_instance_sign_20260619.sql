ALTER TABLE wf_node_config
    ADD COLUMN multi_instance_completion_type INT DEFAULT NULL COMMENT '多实例完成条件类型:1-全部通过,2-任一通过,3-百分比通过' AFTER parallel_reject_strategy,
    ADD COLUMN pass_percentage INT DEFAULT NULL COMMENT '通过百分比阈值(0-100)' AFTER multi_instance_completion_type,
    ADD COLUMN veto_enabled INT DEFAULT 0 COMMENT '是否启用一票否决:0-否,1-是' AFTER pass_percentage;
