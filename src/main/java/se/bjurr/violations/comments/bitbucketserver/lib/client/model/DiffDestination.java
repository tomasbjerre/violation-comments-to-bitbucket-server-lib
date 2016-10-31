package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

public class DiffDestination {
 private final String toString;

 public DiffDestination() {
  toString = null;
 }

 public DiffDestination(String toString) {
  this.toString = toString;
 }

 public String getToString() {
  return toString;
 }

 @Override
 public String toString() {
  return "DiffDestination [toString=" + toString + "]";
 }

}
