package org.jenkinsci.plugins.bbprb;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.triggers.TriggerDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import hudson.model.Run;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.triggers.Trigger;
import java.lang.Exception;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.bbprb.bitbucket.BuildState;

@Extension
public class BitbucketBuildListener extends RunListener<Run<?, ?>> {

  @Override
  public void onStarted(Run build, TaskListener listener) {
    LOGGER.log(Level.FINE, "Started by BitbucketBuildTrigger");

    BitbucketCause cause = (BitbucketCause) build.getCause(BitbucketCause.class);
    if (cause == null) {
      return;
    }

    BitbucketBuildTrigger trigger = getTrigger(build);
    if (trigger == null) {
      return;
    }

    LOGGER.log(Level.FINE, "Started by BitbucketBuildTrigger");
    trigger.setPRState(cause, BuildState.INPROGRESS, build.getUrl());
    try {
      build.setDescription(
          build.getCause(BitbucketCause.class).getShortDescription());
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Could not set build description: {0}",
                 e.getMessage());
    }
  }

  @Override
  public void onCompleted(Run build, TaskListener listener) {
      LOGGER.log(Level.FINE, "Completed after BitbucketBuildTrigger");
    BitbucketBuildTrigger trigger = getTrigger(build);
    if (trigger != null) {
      LOGGER.log(Level.FINE, "Completed after BitbucketBuildTrigger");
      Result result = build.getResult();
      BuildState state;
      if (Result.SUCCESS == result) {
        state = BuildState.SUCCESSFUL;
      } else if (Result.ABORTED == result) {
        state = BuildState.STOPPED;
      } else {
        state = BuildState.FAILED;
      }
      BitbucketCause cause = (BitbucketCause) build.getCause(BitbucketCause.class);
      trigger.setPRState(cause, state, build.getUrl());
    }
  }

  private static final Logger LOGGER =
      Logger.getLogger(BitbucketBuildListener.class.getName());


  private BitbucketBuildTrigger getTrigger(Run r) {
      BitbucketBuildTrigger trigger = null;
      if (r instanceof AbstractBuild) {
          trigger = BitbucketBuildTrigger.getTrigger(((AbstractBuild) r).getProject());
      } else {
          Job job = r.getParent();
          if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
              Map<TriggerDescriptor, Trigger> triggers = ((ParameterizedJobMixIn.ParameterizedJob) job).getTriggers();
              for (Trigger<?> t : triggers.values()) {
                  if (t instanceof BitbucketBuildTrigger) {
                      trigger = (BitbucketBuildTrigger) t;
                  }
              }
          }
      }
      return trigger;
  }

}
