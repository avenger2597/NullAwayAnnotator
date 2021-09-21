package edu.ucr.cs.riple.autofixer.metadata.trackers;

import edu.ucr.cs.riple.injector.Fix;
import java.util.Set;

public interface UsageTracker {

  Set<String> getUsers(Fix fix);

  Set<Usage> getUsage(Fix fix);
}