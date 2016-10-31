package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

import java.util.List;

public class BitbucketServerDiffResponse {
 private final List<BitbucketServerDiff> diffs;

 public BitbucketServerDiffResponse() {
  diffs = null;
 }

 public BitbucketServerDiffResponse(List<BitbucketServerDiff> diffs) {
  this.diffs = diffs;
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
  BitbucketServerDiffResponse other = (BitbucketServerDiffResponse) obj;
  if (diffs == null) {
   if (other.diffs != null) {
    return false;
   }
  } else if (!diffs.equals(other.diffs)) {
   return false;
  }
  return true;
 }

 public List<BitbucketServerDiff> getDiffs() {
  return diffs;
 }

 @Override
 public int hashCode() {
  final int prime = 31;
  int result = 1;
  result = prime * result + (diffs == null ? 0 : diffs.hashCode());
  return result;
 }

 @Override
 public String toString() {
  return "BitbucketServerDiffResponse [diffs=" + diffs + "]";
 }

}
