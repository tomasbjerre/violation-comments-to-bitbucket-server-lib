package se.bjurr.violations.comments.bitbucketserver.lib.client;

public class BitbucketServerComment {

 private final Integer id;
 private final String text;
 private final Integer version;

 public BitbucketServerComment() {
  this.id = null;
  this.text = null;
  this.version = null;
 }

 public BitbucketServerComment(Integer version, String text, Integer id) {
  this.version = version;
  this.text = text;
  this.id = id;
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

}
