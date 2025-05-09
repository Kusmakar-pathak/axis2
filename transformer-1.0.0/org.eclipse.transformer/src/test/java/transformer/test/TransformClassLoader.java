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

package transformer.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Vector;

import aQute.lib.io.ByteBufferInputStream;
import org.eclipse.transformer.TransformException;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.ServiceLoaderConfigActionImpl;
import org.eclipse.transformer.action.impl.ZipActionImpl;
import org.eclipse.transformer.util.FileUtils;
import org.eclipse.transformer.util.SignatureUtils;

public class TransformClassLoader extends ClassLoader {

	public TransformClassLoader(ClassLoader parent, ZipActionImpl jarAction, ClassActionImpl classAction) {
		this(parent, jarAction, classAction, null);
	}

	public TransformClassLoader(ClassLoader parent, ZipActionImpl jarAction, ClassActionImpl classAction,
		ServiceLoaderConfigActionImpl serviceConfigAction) {

		super(parent);

		this.jarAction = jarAction;
		this.classAction = classAction;
		this.serviceConfigAction = serviceConfigAction;
	}

	//

	private final ZipActionImpl jarAction;

	public ZipActionImpl getJarAction() {
		return jarAction;
	}

	public boolean selectResource(String resourceName) {
		return getJarAction().selectResource(resourceName);
	}

	public String getResourceName(String className) {
		return SignatureUtils.classNameToResourceName(className);
	}

	//

	private final ClassActionImpl classAction;

	public ClassActionImpl getClassAction() {
		return classAction;
	}

	public boolean acceptClass(String resourceName) {
		return getClassAction().acceptResource(resourceName);
	}

	public InputStream applyClass(String resourceName, InputStream inputStream) throws TransformException {
		ClassActionImpl classAction = getClassAction();
		ByteData inputData = classAction.collect(resourceName, inputStream);
		ByteData outputData = classAction.apply(inputData);
		return outputData.stream();
	}

	//

	private final ServiceLoaderConfigActionImpl serviceConfigAction;

	public ServiceLoaderConfigActionImpl getServiceConfigAction() {
		return serviceConfigAction;
	}

	public boolean acceptServiceConfig(String resourceName) {
		ServiceLoaderConfigActionImpl configAction = getServiceConfigAction();
		return ((configAction != null) && configAction.acceptResource(resourceName));
	}

	public InputStream applyServiceConfig(String resourceName, InputStream inputStream) throws TransformException {
		ServiceLoaderConfigActionImpl configAction = getServiceConfigAction();
		if (configAction == null) {
			return inputStream;
		} else {
			ByteData inputData = configAction.collect(resourceName, inputStream);
			ByteData outputData = configAction.apply(inputData);
			return outputData.stream();
		}
	}

	//

	public static class TransformClassURLStreamHandler extends URLStreamHandler {
		private final URL baseURL;

		public TransformClassURLStreamHandler(URL baseURL) {
			this.baseURL = baseURL;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new TransformClassURLConnection(baseURL);
		}
	}

	public static class TransformClassURLConnection extends URLConnection {
		public TransformClassURLConnection(URL baseURL) throws IOException {
			super(baseURL);

			this.baseConnection = this.getURL()
				.openConnection();
		}

		//

		private final URLConnection baseConnection;

		public URLConnection getBaseConnection() {
			return baseConnection;
		}

		//

		@Override
		public void connect() throws IOException {
			getBaseConnection().connect();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			URLConnection useBaseConnection = getBaseConnection();
			String baseName = useBaseConnection.getURL()
				.toString();
			InputStream baseStream = useBaseConnection.getInputStream();

			ByteBuffer inputData = FileUtils.read(baseName, baseStream);

			return new ByteBufferInputStream(inputData);
		}
	}

	public static class TransformConfigURLStreamHandler extends URLStreamHandler {
		private final URL baseURL;

		public TransformConfigURLStreamHandler(URL baseURL) {
			this.baseURL = baseURL;
		}

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new TransformConfigURLConnection(baseURL);
		}
	}

	public static class TransformConfigURLConnection extends URLConnection {
		public TransformConfigURLConnection(URL baseURL) throws IOException {
			super(baseURL);

			this.baseConnection = this.getURL()
				.openConnection();
		}

		//

		private final URLConnection baseConnection;

		public URLConnection getBaseConnection() {
			return baseConnection;
		}

		//

		@Override
		public void connect() throws IOException {
			getBaseConnection().connect();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			URLConnection useBaseConnection = getBaseConnection();
			String baseName = useBaseConnection.getURL()
				.toString();
			InputStream baseStream = useBaseConnection.getInputStream();

			ByteBuffer inputData = FileUtils.read(baseName, baseStream);

			return new ByteBufferInputStream(inputData);
		}
	}

	//

	@Override
	public InputStream getResourceAsStream(String resourceName) {
		InputStream baseStream = super.getResourceAsStream(resourceName);
		if (baseStream == null) {
			System.out.println("Get resource stream: " + resourceName + ": No base stream");
			return null;

		} else if (!selectResource(resourceName)) {
			System.out.println("Get resource stream: " + resourceName + ": Not selected");
			return baseStream;

		} else if (acceptClass(resourceName)) {
			System.out.println("Get resource stream: " + resourceName + ": Accepted as class");
			try {
				return applyClass(resourceName, baseStream);

			} catch (TransformException e) {
				System.err.println("Class transform failure [ " + resourceName + " ]");
				e.printStackTrace();

				return super.getResourceAsStream(resourceName);
			}

		} else if (acceptServiceConfig(resourceName)) {
			System.out.println("Get resource stream: " + resourceName + ": Accepted as service config");

			try {
				return applyServiceConfig(resourceName, baseStream);

			} catch (TransformException e) {
				System.err.println("Servic configuration transform failure [ " + resourceName + " ]");
				e.printStackTrace();

				return super.getResourceAsStream(resourceName);
			}

		} else {
			System.out.println("Get resource stream: " + resourceName + ": Not accepted");
			return baseStream;
		}
	}

	protected URL transformAsClass(URL baseURL) {
		String baseText = baseURL.toString();
		try {
			return new URL(null, baseText, new TransformClassURLStreamHandler(baseURL));
		} catch (MalformedURLException e) {
			System.err.println("Failed to wrap base URL [ " + baseText + " ] for class transformation: " + e);
			return baseURL;
		}
	}

	protected URL transformAsServiceConfig(URL baseURL) {
		String baseText = baseURL.toString();
		try {
			return new URL(null, baseText, new TransformConfigURLStreamHandler(baseURL));
		} catch (MalformedURLException e) {
			System.err
				.println("Failed to wrap base URL [ " + baseText + " ] for service configuration transformation: " + e);
			return baseURL;
		}
	}

	@Override
	protected URL findResource(String name) {
		URL baseURL = super.findResource(name);
		if (baseURL == null) {
			return null;
		} else {
			return transform(name, baseURL);
		}
	}

	protected URL transform(String name, URL baseURL) {
		if (!selectResource(name)) {
			return baseURL;
		} else if (acceptClass(name)) {
			return transformAsClass(baseURL);
		} else if (acceptServiceConfig(name)) {
			return transformAsServiceConfig(baseURL);
		} else {
			return baseURL;
		}
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		Enumeration<URL> baseURLs = super.findResources(name);
		if (!baseURLs.hasMoreElements()) {
			return baseURLs;

		} else if (!selectResource(name)) {
			return baseURLs;

		} else {
			Vector<URL> transformedURLs = new Vector<>();
			while (baseURLs.hasMoreElements()) {
				transformedURLs.add(transform(name, baseURLs.nextElement()));
			}
			return transformedURLs.elements();
		}
	}

	@Override
	protected Class<?> loadClass(String className, boolean resolveClass) throws ClassNotFoundException {
		String resourceName = getResourceName(className);

		System.out.println("Load [ " + className + " ] as [ " + resourceName + " ]");

		if (!selectResource(resourceName)) {
			System.out.println("Load [ " + className + " ]: Not selected");
			return super.loadClass(className, resolveClass);
		}

		Class<?> loadedClass = findLoadedClass(className);
		if (loadedClass == null) {
			System.out.println("Load [ " + className + " ]: Not previously loaded: Find");
			loadedClass = findClass(className);
		} else {
			System.out.println("Load [ " + className + " ]: Previously loaded");
		}

		if (resolveClass) {
			System.out.println("Load [ " + className + " ]: Resolve");
			resolveClass(loadedClass);
		}

		return loadedClass;
	}

	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		String resourceName = getResourceName(className);

		System.out.println("Find [ " + className + " ] as [ " + resourceName + " ]");

		if (!selectResource(resourceName)) {
			System.out.println("Find [ " + className + " ]: Not selected");
			return super.findClass(className);
		}

		Class<?> loadedClass = findLoadedClass(className);
		if (loadedClass != null) {
			System.out.println("Find [ " + className + " ]: Previously loaded");
			return loadedClass;
		}

		InputStream classStream = getResourceAsStream(resourceName);
		if (classStream == null) {
			System.out.println("Find [ " + className + " ]: Not found");
			throw new ClassNotFoundException(className);
		}

		System.out.println("Find [ " + className + " ]: Reading class stream.");
		ByteBuffer classData;
		try {
			classData = FileUtils.read(className, classStream);
		} catch (IOException e) {
			throw new ClassNotFoundException(className, e);
		}

		System.out.println("Find [ " + className + " ]: Define class.");
		return super.defineClass(className, classData.array(), classData.arrayOffset(), classData.limit());
	}
}
