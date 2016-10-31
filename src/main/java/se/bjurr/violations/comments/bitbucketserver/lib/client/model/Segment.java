package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

import java.util.List;

public class Segment {
 private final DIFFTYPE type;
 private final List<Line> lines;

 public Segment() {
  type = null;
  lines = null;
 }

 public Segment(DIFFTYPE type, List<Line> lines) {
  this.type = type;
  this.lines = lines;
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
  Segment other = (Segment) obj;
  if (lines == null) {
   if (other.lines != null) {
    return false;
   }
  } else if (!lines.equals(other.lines)) {
   return false;
  }
  if (type != other.type) {
   return false;
  }
  return true;
 }

 public List<Line> getLines() {
  return lines;
 }

 public DIFFTYPE getType() {
  return type;
 }

 @Override
 public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + (lines == null ? 0 : lines.hashCode());
  result = prime * result + (type == null ? 0 : type.hashCode());
  return result;
 }

 @Override
 public String toString() {
  return "Segment [type=" + type + ", lines=" + lines + "]";
 }
}
