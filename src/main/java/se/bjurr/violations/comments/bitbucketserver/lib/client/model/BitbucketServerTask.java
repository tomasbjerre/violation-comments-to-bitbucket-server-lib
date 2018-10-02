package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

public class BitbucketServerTask {
  private final Integer id;

  private final String text;

  public BitbucketServerTask(Integer id, String text) {
    this.text = text;
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public String getText() {
    return text;
  }
}
