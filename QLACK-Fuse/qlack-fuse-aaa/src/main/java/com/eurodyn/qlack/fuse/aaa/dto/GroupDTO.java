package com.eurodyn.qlack.fuse.aaa.dto;

import java.io.Serializable;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author European Dynamics SA
 */
@Getter
@Setter
@NoArgsConstructor
public class GroupDTO implements Serializable {

  private String id;
  private String name;
  private String objectID;
  private String description;
  private GroupDTO parent;
  private Set<GroupDTO> children;

  public GroupDTO(String id) {
    this.id = id;
  }

}
