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

package org.eclipse.transformer.action.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import aQute.lib.io.IO;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.util.FileUtils;

// This implementation is retained as distinct from the interface.
// That is because ElementAction.apply exposes ByteData as a type.
//
// The interface and implementation are place with the action packages,
// not the utility packages, because this type and its usage are specific
// to the actions.

public class ByteDataImpl implements ByteData {
	/**
	 * Associate a name with byte data.
	 * <p>
	 * Usually, the name is a file or archive entry name and the byte data is
	 * the contents of the file or entry.
	 *
	 * @param name A name associated with the data.
	 * @param buffer Byte data associated with the name.
	 * @param charset The charset for the byte data.
	 */
	public ByteDataImpl(String name, ByteBuffer buffer, Charset charset) {
		this.name = name;
		this.buffer = buffer;
		this.charset = charset;
	}

	@Override
	public String toString() {
		return super.toString() + "(name=\"" + name + "\", buffer=" + buffer + ", charset=" + charset + ")";
	}

	private final String		name;
	private final ByteBuffer	buffer;
	private final Charset		charset;

	@Override
	public String name() {
		return name;
	}

	@Override
	public ByteBuffer buffer() {
		return buffer.duplicate();
	}

	@Override
	public int length() {
		return buffer.remaining();
	}

	//

	@Override
	public InputStream stream() {
		return FileUtils.stream(buffer());
	}

	@Override
	public Reader reader() {
		return FileUtils.reader(buffer(), charset());
	}

	@Override
	public Charset charset() {
		return charset;
	}

	@Override
	public OutputStream writeTo(OutputStream outputStream) throws IOException {
		return IO.copy(buffer(), outputStream);
	}

	//

	@Override
	public ByteDataImpl copy() {
		return copy(name);
	}

	@Override
	public ByteDataImpl copy(String name) {
		return new ByteDataImpl(name, buffer(), charset());
	}
}
