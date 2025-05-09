/********************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/


import java.util.jar.JarEntry
import java.util.jar.JarFile

import static org.assertj.core.api.Assertions.assertThat

//println " basedir: ${basedir}"
//println " localRepositoryPath: ${localRepositoryPath}"
//println " mavenVersion: ${mavenVersion}"

File artifact_main = new File(basedir, "target/shading-example-1.0.0.jar")
assertThat(artifact_main).isFile()

new JarFile(artifact_main).withCloseable { jarFile ->
	JarEntry shaded_class = jarFile.getJarEntry("shaded/conditioner/Conditioner.class");
	assertThat(shaded_class).isNotNull();
	JarEntry old_class = jarFile.getJarEntry("conditioner/Conditioner.class");
	assertThat(old_class).isNull();

	JarEntry shaded_resource = jarFile.getJarEntry("shaded/conditioner/resource.foo");
	assertThat(shaded_resource).isNotNull();
	JarEntry old_resource = jarFile.getJarEntry("conditioner/resource.foo");
	assertThat(old_resource).isNull();

	JarEntry shaded_archive = jarFile.getJarEntry("shaded/conditioner/sac-1.3.jar");
	assertThat(shaded_archive).isNotNull();
	JarEntry old_archive = jarFile.getJarEntry("conditioner/sac-1.3.jar");
	assertThat(old_archive).isNull();
}

return true
