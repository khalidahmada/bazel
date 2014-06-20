// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.blaze.commands;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.devtools.build.docgen.BlazeRuleHelpPrinter;
import com.google.devtools.build.lib.blaze.BlazeCommand;
import com.google.devtools.build.lib.blaze.BlazeCommandUtils;
import com.google.devtools.build.lib.blaze.BlazeModule;
import com.google.devtools.build.lib.blaze.BlazeRuntime;
import com.google.devtools.build.lib.blaze.BlazeVersionInfo;
import com.google.devtools.build.lib.blaze.Command;
import com.google.devtools.build.lib.util.ExitCode;
import com.google.devtools.build.lib.util.io.OutErr;
import com.google.devtools.build.lib.view.ConfiguredRuleClassProvider;
import com.google.devtools.common.options.Converters;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The 'blaze help' command, which prints all available commands as well as
 * specific help pages.
 */
@Command(name = "help",
         options = { HelpCommand.Options.class },
         allowResidue = true,
         mustRunInWorkspace = false,
         shortDescription = "Prints help for commands, or the index.",
         help = "resource:help.txt")
public final class HelpCommand implements BlazeCommand {
  public static class Options extends OptionsBase {

    @Option(name = "help_verbosity",
            category = "help",
            defaultValue = "medium",
            converter = Converters.HelpVerbosityConverter.class,
            help = "Select the verbosity of the help command.")
    public OptionsParser.HelpVerbosity helpVerbosity;

    @Option(name = "long",
            abbrev = 'l',
            defaultValue = "null",
            category = "help",
            expansion = {"--help_verbosity", "long"},
            help = "Show full description of each option, instead of just its name.")
    public Void showLongFormOptions;

    @Option(name = "short",
            defaultValue = "null",
            category = "help",
            expansion = {"--help_verbosity", "short"},
            help = "Show only the names of the options, not their types or meanings.")
    public Void showShortFormOptions;
  }

  private static final Map<String, String> BLAZE_OPTION_CATEGORIES;
  static {
    Map<String, String> m = BLAZE_OPTION_CATEGORIES = Maps.newHashMap();
    m.put("checking",
          "Checking options, which control Blaze's error checking and/or warnings");
    m.put("coverage",
          "Options that affect how Blaze generates code coverage information");
    m.put("experimental",
          "Experimental options, which control experimental (and potentially risky) features");
    m.put("flags",
          "Flags options, for passing options to other tools");
    m.put("help",
          "Help options");
    m.put("host jvm startup",
          "Options that affect the startup of the Blaze server's JVM");
    m.put("misc",
          "Miscellaneous options");
    m.put("package loading",
          "Options that specify how to locate packages");
    m.put("query",
          "Options affecting the 'blaze query' dependency query command");
    m.put("run",
          "Options specific to 'blaze run'");
    m.put("semantics",
          "Semantics options, which affect the build commands and/or output file contents");
    m.put("server startup",
          "Startup options, which affect the startup of the Blaze server");
    m.put("strategy",
          "Strategy options, which affect how Blaze will execute the build");
    m.put("testing",
          "Options that affect how Blaze runs tests");
    m.put("verbosity",
          "Verbosity options, which control what Blaze prints");
    m.put("version",
          "Version options, for selecting which version of other tools will be used");
    m.put("what",
          "Output selection options, for determining what to build/test");
  }

  @Override
  public void editOptions(BlazeRuntime runtime, OptionsParser optionsParser) {}

  @Override
  public ExitCode exec(BlazeRuntime runtime, OptionsProvider options, OutErr outErr) {
    Options helpOptions = options.getOptions(Options.class);
    if (options.getResidue().isEmpty()) {
      emitBlazeVersionInfo(outErr);
      emitGenericHelp(runtime, outErr);
      return ExitCode.SUCCESS;
    }
    if (options.getResidue().size() != 1) {
      runtime.getReporter().error(null, "You must specify exactly one command");
      return ExitCode.COMMAND_LINE_ERROR;
    }
    String helpSubject = options.getResidue().get(0);
    if (helpSubject.equals("startup_options")) {
      emitBlazeVersionInfo(outErr);
      emitStartupOptions(outErr, helpOptions.helpVerbosity, runtime);
      return ExitCode.SUCCESS;
    } else if (helpSubject.equals("target-syntax")) {
      emitBlazeVersionInfo(outErr);
      emitTargetSyntaxHelp(outErr);
      return ExitCode.SUCCESS;
    } else if (helpSubject.equals("info-keys")) {
      emitInfoKeysHelp(runtime, outErr);
      return ExitCode.SUCCESS;
    }

    BlazeCommand command = runtime.getCommandMap().get(helpSubject);
    if (command == null) {
      ConfiguredRuleClassProvider provider = runtime.getRuleClassProvider();
      if (provider.getRuleClassMap().containsKey(helpSubject)) {
        // There is a rule with a corresponding name
        outErr.printOut(BlazeRuleHelpPrinter.getRuleDoc(helpSubject, provider));
        return ExitCode.SUCCESS;
      } else {
        runtime.getReporter().error(
            null, "'" + helpSubject + "' is neither a command nor a build rule");
        return ExitCode.COMMAND_LINE_ERROR;
      }
    }
    emitBlazeVersionInfo(outErr);
    outErr.printOut(BlazeCommandUtils.getUsage(
        command.getClass(), BLAZE_OPTION_CATEGORIES, helpOptions.helpVerbosity,
        runtime.getBlazeModules(),
        runtime.getRuleClassProvider()));
    return ExitCode.SUCCESS;
  }

  private void emitBlazeVersionInfo(OutErr outErr) {
    String releaseInfo = BlazeVersionInfo.instance().getReleaseName();
    String line = "[Blaze " + releaseInfo + "]";
    outErr.printOut(String.format("%80s\n", line));
  }

  @SuppressWarnings("unchecked") // varargs generic array creation
  private void emitStartupOptions(OutErr outErr, OptionsParser.HelpVerbosity helpVerbosity,
      BlazeRuntime runtime) {
    outErr.printOut(
        BlazeCommandUtils.expandHelpTopic("startup_options",
            "resource:startup_options.txt",
            getClass(),
            BlazeCommandUtils.getStartupOptions(runtime.getBlazeModules()),
        BLAZE_OPTION_CATEGORIES,
        helpVerbosity));
  }

  private void emitTargetSyntaxHelp(OutErr outErr) {
    outErr.printOut(BlazeCommandUtils.expandHelpTopic("target-syntax",
                                    "resource:target-syntax.txt",
                                    getClass(),
                                    ImmutableList.<Class<? extends OptionsBase>>of(),
                                    BLAZE_OPTION_CATEGORIES,
                                    OptionsParser.HelpVerbosity.MEDIUM));
  }

  private void emitInfoKeysHelp(BlazeRuntime runtime, OutErr outErr) {
    for (InfoKey key : InfoKey.values()) {
      outErr.printOut(String.format("%-23s %s\n", key.getName(), key.getDescription()));
    }

    for (BlazeModule.InfoItem item : InfoCommand.getInfoItemMap(runtime,
        OptionsParser.newOptionsParser(
            ImmutableList.<Class<? extends OptionsBase>>of())).values()) {
      outErr.printOut(String.format("%-23s %s\n", item.getName(), item.getDescription()));
    }
  }

  private void emitGenericHelp(BlazeRuntime runtime, OutErr outErr) {
    outErr.printOut("Usage: blaze <command> <options> ...\n\n");

    outErr.printOut("Available commands:\n");

    Map<String, BlazeCommand> commandsByName = runtime.getCommandMap();
    List<String> namesInOrder = new ArrayList<>(commandsByName.keySet());
    Collections.sort(namesInOrder);

    for (String name : namesInOrder) {
      BlazeCommand command = commandsByName.get(name);
      Command annotation = command.getClass().getAnnotation(Command.class);
      if (annotation.hidden()) {
        continue;
      }

      String shortDescription = annotation.shortDescription();
      outErr.printOut(String.format("  %-19s %s\n", name, shortDescription));
    }

    outErr.printOut("\n");
    outErr.printOut("Getting more help:\n");
    outErr.printOut("  blaze help <command>\n");
    outErr.printOut("                   Prints help and options for <command>.\n");
    outErr.printOut("  blaze help startup_options\n");
    outErr.printOut("                   Options for the JVM hosting Blaze.\n");
    outErr.printOut("  blaze help target-syntax\n");
    outErr.printOut("                   Explains the syntax for specifying targets.\n");
    outErr.printOut("  blaze help info-keys\n");
    outErr.printOut("                   Displays a list of keys used by the info command.\n");
  }
}
