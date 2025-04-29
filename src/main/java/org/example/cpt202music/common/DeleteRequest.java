package org.example.cpt202music.common;

import lombok.Data;

import java.io.Serializable;


// A generic class for deletion requests
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
