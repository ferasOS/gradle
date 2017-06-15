/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.launcher.daemon.configuration;

import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.internal.jvm.JavaInfo;
import org.gradle.process.internal.CurrentProcess;
import org.gradle.process.internal.JvmOptions;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class BuildProcess extends CurrentProcess {

    public BuildProcess() {
        super();
    }

    protected BuildProcess(JavaInfo jvm, JvmOptions effectiveJvmOptions) {
        super(jvm, effectiveJvmOptions);
    }

    /**
     * Attempts to configure the current process to run with the required build parameters.
     *
     * If the required parameters contain explicitly defined immutable jvm args,
     * all of those immutable args have to match the current process.
     * If the required parameters contain explicitly defined immutable system properties,
     * all of those immutable system properties have to match the current process.
     *
     * @return True if the current process could be configured, false otherwise.
     */
    public boolean configureForBuild(DaemonParameters requiredBuildParameters) {
        boolean javaHomeMatch = getJvm().equals(requiredBuildParameters.getEffectiveJvm());

        List<String> currentImmutables;
        List<String> requiredImmutables;
        if (requiredBuildParameters.hasUserDefinedImmutableJvmArgs() && requiredBuildParameters.hasUserDefinedImmutableSystemProperties()) {
            currentImmutables = getJvmOptions().getAllImmutableJvmArgs();
            requiredImmutables = requiredBuildParameters.getEffectiveSingleUseJvmArgs();
        } else if (requiredBuildParameters.hasUserDefinedImmutableJvmArgs()) {
            final JvmOptions ignoreSysPropJvmOptions = new JvmOptions(new IdentityFileResolver());
            ignoreSysPropJvmOptions.setJvmArgs(getJvmOptions().getAllImmutableJvmArgs());
            ignoreSysPropJvmOptions.setSystemProperties(requiredBuildParameters.getImmutableSystemProperties());
            currentImmutables = ignoreSysPropJvmOptions.getAllImmutableJvmArgs();
            requiredImmutables = requiredBuildParameters.getEffectiveSingleUseJvmArgs();
        } else if (requiredBuildParameters.hasUserDefinedImmutableSystemProperties()) {
            final JvmOptions sysPropOnlyJvmOptions = new JvmOptions(new IdentityFileResolver());
            sysPropOnlyJvmOptions.systemProperties(getJvmOptions().getImmutableSystemProperties());
            currentImmutables = sysPropOnlyJvmOptions.getAllImmutableJvmArgs();
            requiredImmutables = requiredBuildParameters.getEffectiveSingleUseJvmArgs();
        } else {
            currentImmutables = Collections.emptyList();
            requiredImmutables = Collections.emptyList();
        }
        boolean noChangeToImmutableJvmArgsRequired = requiredImmutables.equals(currentImmutables);

        if (javaHomeMatch && noChangeToImmutableJvmArgsRequired) {
            // Set the system properties and use this process
            Properties properties = new Properties();
            properties.putAll(requiredBuildParameters.getEffectiveSystemProperties());
            System.setProperties(properties);
            return true;
        }
        return false;
    }
}
