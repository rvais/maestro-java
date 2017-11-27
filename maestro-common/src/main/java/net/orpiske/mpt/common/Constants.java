/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.orpiske.mpt.common;

import java.io.File;

/**
 * Application constants
 *
 */
public final class Constants {

	public static final String VERSION = "1.0.0";

	public static final String BIN_NAME = "maestro-java";

	public static final String HOME_PROPERTY = "net.orpiske.mpt.maestro.home";

	public static final String HOME_DIR;

	public static final String MAESTRO_CONFIG_DIR;

	static {
		HOME_DIR = System.getProperty(HOME_PROPERTY);

		MAESTRO_CONFIG_DIR = System.getProperty(HOME_PROPERTY) + File.separator + "config";
	}


	/**
	 * Restricted constructor
	 */
	private Constants() {}


}
