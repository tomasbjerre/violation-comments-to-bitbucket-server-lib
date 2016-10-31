package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

public class Line {
 private final Integer destination;

 public Line() {
  destination = null;
 }

 public Line(Integer destination) {
  this.destination = destination;
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
  Line other = (Line) obj;
  if (destination == null) {
   if (other.destination != null) {
    return false;
   }
  } else if (!destination.equals(other.destination)) {
   return false;
  }
  return true;
 }

 public Integer getDestination() {
  return destination;
 }

 @Override
 public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + (destination == null ? 0 : destination.hashCode());
  return result;
 }

 @Override
 public String toString() {
  return "Line [destination=" + destination + "]";
 }

}
