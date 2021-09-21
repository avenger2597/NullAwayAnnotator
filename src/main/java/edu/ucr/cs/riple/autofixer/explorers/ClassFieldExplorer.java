package edu.ucr.cs.riple.autofixer.explorers;

import edu.ucr.cs.riple.autofixer.AutoFixer;
import edu.ucr.cs.riple.autofixer.FixType;
import edu.ucr.cs.riple.autofixer.Report;
import edu.ucr.cs.riple.autofixer.metadata.index.Bank;
import edu.ucr.cs.riple.autofixer.metadata.index.Error;
import edu.ucr.cs.riple.autofixer.metadata.index.FixEntity;
import edu.ucr.cs.riple.injector.Fix;
import java.util.List;

public class ClassFieldExplorer extends AdvancedExplorer {

  public ClassFieldExplorer(
      AutoFixer autoFixer, List<Fix> fixes, Bank<Error> errorBank, Bank<FixEntity> fixBank) {
    super(autoFixer, fixes, errorBank, fixBank, FixType.CLASS_FIELD);
  }

  @Override
  protected void init() {
    this.tracker = autoFixer.fieldUsageTracker;
    System.out.println("Trying to find groups for Class Field fixes");
    fixGraph.updateUsages(tracker);
    fixGraph.findGroups();
  }

  @Override
  protected Report effectByScope(Fix fix) {
    return super.effectByScope(fix, tracker.getUsers(fix));
  }

  @Override
  public boolean isApplicable(Fix fix) {
    return fix.location.equals(fixType.name);
  }
}