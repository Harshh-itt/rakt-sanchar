package com.example.blood.model;

import java.time.Instant;
import java.util.Objects;

public class EarnedBadge {
    private BadgeCode code;
    private Instant earnedAt;

    public EarnedBadge() {
    }

    public EarnedBadge(BadgeCode code, Instant earnedAt) {
        this.code = code;
        this.earnedAt = earnedAt;
    }

    public BadgeCode getCode() {
        return code;
    }

    public void setCode(BadgeCode code) {
        this.code = code;
    }

    public Instant getEarnedAt() {
        return earnedAt;
    }

    public void setEarnedAt(Instant earnedAt) {
        this.earnedAt = earnedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EarnedBadge that)) return false;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
