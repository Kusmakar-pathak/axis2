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

package org.eclipse.transformer.bnd.analyzer;

import java.net.URL;
import java.util.Map;
import java.util.function.Function;

import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.externalplugin.ExternalPlugin;
import org.eclipse.transformer.jakarta.JakartaTransform;

/**
 * Bnd Analyzer Plugin for Transformer configured with Jakarta rules.
 */
@ExternalPlugin(name = "JakartaTransformer", objectClass = AnalyzerPlugin.class)
public class JakartaTransformerAnalyzerPlugin extends TransformerAnalyzerPlugin implements AnalyzerPlugin {
	public JakartaTransformerAnalyzerPlugin() {
		super();
	}

	@Override
	protected Map<String, String> getOptionDefaults() {
		return JakartaTransform.getOptionDefaults();
	}

	@Override
	protected Function<String, URL> getRuleLoader() {
		return JakartaTransform.getRuleLoader();
	}
}
