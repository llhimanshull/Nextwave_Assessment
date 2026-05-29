package com.minijira.enums;

import java.util.Set;

/**
 * TASK STATUS ENUM WITH ENFORCED TRANSITION RULES.
 *
 * VALID TRANSITIONS:
 *   TODO       -> IN_PROGRESS, BLOCKED
 *   IN_PROGRESS -> IN_REVIEW, BLOCKED
 *   IN_REVIEW  -> DONE, BLOCKED
 *   BLOCKED    -> TODO, IN_PROGRESS   (ALLOWS RECOVERY BACK INTO ACTIVE FLOW)
 *   DONE       -> (NO FURTHER TRANSITIONS ALLOWED - TERMINAL STATE)
 *
 * ONLY THE ASSIGNEE OR A MANAGER CAN ADVANCE A TASK STATUS - ENFORCED IN SERVICE LAYER
 */
public enum TaskStatus {

    TODO {
        @Override
        public Set<TaskStatus> allowedTransitions() {
            return Set.of(IN_PROGRESS, BLOCKED);
        }
    },
    IN_PROGRESS {
        @Override
        public Set<TaskStatus> allowedTransitions() {
            return Set.of(IN_REVIEW, BLOCKED);
        }
    },
    IN_REVIEW {
        @Override
        public Set<TaskStatus> allowedTransitions() {
            return Set.of(DONE, BLOCKED);
        }
    },
    BLOCKED {
        @Override
        public Set<TaskStatus> allowedTransitions() {
            // BLOCKED CAN RECOVER BACK INTO TODO OR IN_PROGRESS
            return Set.of(TODO, IN_PROGRESS);
        }
    },
    DONE {
        @Override
        public Set<TaskStatus> allowedTransitions() {
            // DONE IS A TERMINAL STATE - NO FURTHER TRANSITIONS ALLOWED
            return Set.of();
        }
    };

    public abstract Set<TaskStatus> allowedTransitions();

    public boolean canTransitionTo(TaskStatus next) {
        return this.allowedTransitions().contains(next);
    }
}
