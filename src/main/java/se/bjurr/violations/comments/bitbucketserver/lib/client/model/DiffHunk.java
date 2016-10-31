package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

import java.util.List;

public class DiffHunk {
 private final List<Segment> segments;

 public DiffHunk() {
  segments = null;
 }

 public DiffHunk(List<Segment> segments) {
  this.segments = segments;
 }

 @Override
 public boolean equals(Object obj) {
  if (this == obj) {
   return true;
  }
  if (obj == null) {
   return false;
  }
  if (getClass() != obj.getClass()) {
   return false;
  }
  DiffHunk other = (DiffHunk) obj;
  if (segments == null) {
   if (other.segments != null) {
    return false;
   }
  } else if (!segments.equals(other.segments)) {
   return false;
  }
  return true;
 }

 public List<Segment> getSegments() {
  return segments;
 }

 @Override
 public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + (segments == null ? 0 : segments.hashCode());
  return result;
 }

 @Override
 public String toString() {
  return "DiffHunk [segments=" + segments + "]";
 }
}
