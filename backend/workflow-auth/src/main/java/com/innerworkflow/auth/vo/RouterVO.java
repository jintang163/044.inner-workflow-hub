package com.innerworkflow.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RouterVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String path;

    private String component;

    private String redirect;

    private MetaVO meta;

    private List<RouterVO> children;

    @Data
    public static class MetaVO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String title;

        private String icon;

        private Integer sortOrder;

        private Boolean visible;

        private Boolean keepAlive;
    }
}
