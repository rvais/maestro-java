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
package net.orpiske.mpt.utils;


import net.orpiske.mpt.utils.contrib.net.URLUtils;
import net.orpiske.mpt.utils.contrib.resource.HttpResourceExchange;
import net.orpiske.mpt.utils.contrib.resource.Resource;
import net.orpiske.mpt.utils.contrib.resource.ResourceExchange;
import net.orpiske.mpt.utils.contrib.resource.ResourceInfo;
import net.orpiske.mpt.utils.contrib.resource.exceptions.ResourceExchangeException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implements the download function.
 *
 * @author Otavio R. Piske <angusyoung@gmail.com>
 *
 */
public class Downloader {

	private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

	private static void copy(final Resource<InputStream> resource,
			final OutputStream output) throws IOException {
		InputStream input = resource.getPayload();

		long i = 0;
		long total = resource.getResourceInfo().getSize();

		for (i = 0; i < total; i++) {
			output.write(input.read());

			if ((i % (1024 * 512)) == 0) {
				double percentComplete = 0;

				if (i > 0) {
					percentComplete = i / ((double) total /100.0);
				}
				else {
					percentComplete = 0.0;
				}


				System.out.print("\r" + (int) percentComplete + "% complete (" + i + " of "
						+ total + ")");
			}
		}

		System.out.print("\r100% complete (" + i + " of " + total + ")\n");

		output.flush();

	}



	/**
	 * Setups the output file
	 * @param url file URL
	 * @param overwrite whether to overwrite existent files
	 * @return A new File object pointing to the output file
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws IOException if unable to create the output directory, file or remove an
	 * existent file
	 */
	private static File setupOutputFile(String url, final String destDir, boolean overwrite)
			throws MalformedURLException, URISyntaxException, IOException {
		String fileName = URLUtils.getFilename(url);
		String fullName = destDir + File.separator + fileName;

		File outputFile = new File(fullName);

		if (!outputFile.getParentFile().exists()) {
			if (!outputFile.getParentFile().mkdirs()) {
				throw new IOException("Unable to create output directory " + fullName);
			}
		}

		if (!outputFile.exists()) {
			if (!outputFile.createNewFile()) {
				throw new IOException("Unable to create file " + fullName);
			}
		} else {
			if (overwrite) {
				if (outputFile.delete()) {
					if (!outputFile.createNewFile()) {
						throw new IOException("Unable to create file " + fullName);
					}
				}
				else {
					throw new IOException("Unable to delete existing file " + fullName);
				}
			} else {
				logger.info("Destination file " + fullName + " already exists");
			}
		}
		return outputFile;
	}

	/**
	 * Saves the downloaded file
	 * @param outputFile the output file
	 * @param resource the resource to save
	 * @throws IOException if the file is not found or unable to save the file
	 */
	private static void saveDownload(File outputFile, Resource<InputStream> resource)
			throws IOException {
		FileOutputStream output = null;

		try {
			output = new FileOutputStream(outputFile);

			copy(resource, output);
			long lastModified = resource.getResourceInfo().getLastModified();
			if (!outputFile.setLastModified(lastModified)) {
				logger.info("Unable to set the last modified date for " +
						outputFile.getPath());
			}
		} finally {

			IOUtils.closeQuietly(output);
			IOUtils.closeQuietly(resource.getPayload());
		}
	}



	/**
	 * Download a file
	 * @param url the URL to the file
	 * @param overwrite whether or not to overwrite existent files
	 * @throws ResourceExchangeException if unable to download the file
	 */
	public static void download(final String url, final String destDir, boolean overwrite) throws ResourceExchangeException {
		try {
			URI uri = new URI(url);

			File outputFile = setupOutputFile(url, destDir, overwrite);

			ResourceExchange resourceExchange = new HttpResourceExchange();
			ResourceInfo resourceInfo = resourceExchange.info(uri);

			try {
				long outSize = FileUtils.sizeOf(outputFile);
				long sourceSize = resourceInfo.getSize();

				if (sourceSize == outSize) {
					logger.info("Destination file and source file appears to be " +
							"the same. Using cached file instead.");

				} else {
					Resource<InputStream> resource = resourceExchange.get(uri);

					saveDownload(outputFile, resource);

					if (logger.isDebugEnabled()) {
						logger.debug("Downloaded " + outputFile.getPath());
					}
				}
			} finally {
				logger.debug("Releasing resources");
				resourceExchange.release();
			}
		} catch (URISyntaxException e) {
			throw new ResourceExchangeException("Invalid URI: " + url, e);
		} catch (IOException e) {
			throw new ResourceExchangeException("I/O error: " + e.getMessage(), e);
		}
	}


	/**
	 * Download a file overwriting existent ones
	 * @param url the URL to the file
	 * @throws ResourceExchangeException if unable to download the file
	 */
	public static void download(final String url, final String destDir) throws ResourceExchangeException {
		download(url, destDir, false);
	}
}
