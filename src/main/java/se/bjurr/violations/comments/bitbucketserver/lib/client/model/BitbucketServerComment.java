package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

import java.util.ArrayList;
import java.util.List;

public class BitbucketServerComment {

  private final Integer id;
  private final String text;
  private final Integer version;
  private List<BitbucketServerTask> tasks;
  private List<BitbucketServerComment> comments;

  public BitbucketServerComment() {
    this.id = null;
    this.text = null;
    this.version = null;
    this.tasks = null;
    this.comments = null;
  }

  public BitbucketServerComment(Integer version, String text, Integer id) {
    this.version = version;
    this.text = text;
    this.id = id;
    this.tasks = new ArrayList<>();
    this.comments = new ArrayList<>();
  }

  public Integer getId() {
    return this.id;
  }

  public String getText() {
    return this.text;
  }

  public Integer getVersion() {
    return this.version;
  }

  public List<BitbucketServerTask> getTasks() {
    return tasks;
  }

  public List<BitbucketServerComment> getComments() {
    return comments;
  }

  public void setTasks(List<BitbucketServerTask> tasks) {
    this.tasks = tasks;
  }

  public void setComments(List<BitbucketServerComment> comments) {
    this.comments = comments;
  }
}
