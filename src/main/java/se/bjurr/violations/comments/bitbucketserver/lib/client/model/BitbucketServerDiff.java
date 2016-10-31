package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

import java.util.List;

public class BitbucketServerDiff {
 private final DiffDestination destination;
 private final List<DiffHunk> hunks;

 public BitbucketServerDiff() {
  destination = null;
  hunks = null;
 }

 public BitbucketServerDiff(DiffDestination destination, List<DiffHunk> hunks) {
  this.destination = destination;
  this.hunks = hunks;
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
  BitbucketServerDiff other = (BitbucketServerDiff) obj;
  if (destination == null) {
   if (other.destination != null) {
    return false;
   }
  } else if (!destination.equals(other.destination)) {
   return false;
  }
  if (hunks == null) {
   if (other.hunks != null) {
    return false;
   }
  } else if (!hunks.equals(other.hunks)) {
   return false;
  }
  return true;
 }

 public DiffDestination getDestination() {
  return destination;
 }

 public List<DiffHunk> getHunks() {
  return hunks;
 }

 @Override
 public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + (destination == null ? 0 : destination.hashCode());
  result = prime * result + (hunks == null ? 0 : hunks.hashCode());
  return result;
 }

 @Override
 public String toString() {
  return "BitbucketServerDiff [destination=" + destination + ", hunks=" + hunks + "]";
 }
}
