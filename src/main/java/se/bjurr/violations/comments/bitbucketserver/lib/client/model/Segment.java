package se.bjurr.violations.comments.bitbucketserver.lib.client.model;

import java.util.List;

public class Segment {
  private final DIFFTYPE type;
  private final List<Line> lines;

  public Segment() {
    this.type = null;
    this.lines = null;
  }

  public Segment(final DIFFTYPE type, final List<Line> lines) {
    this.type = type;
    this.lines = lines;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final Segment other = (Segment) obj;
    if (this.lines == null) {
      if (other.lines != null) {
        return false;
      }
    } else if (!this.lines.equals(other.lines)) {
      return false;
    }
    return this.type == other.type;
  }

  public List<Line> getLines() {
    return this.lines;
  }

  public DIFFTYPE getType() {
    return this.type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.lines == null ? 0 : this.lines.hashCode());
    result = prime * result + (this.type == null ? 0 : this.type.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "Segment [type=" + this.type + ", lines=" + this.lines + "]";
  }
}
