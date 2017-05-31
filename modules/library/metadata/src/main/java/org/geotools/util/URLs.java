/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;

/**
 * Utilities for manipulating and converting to and from {@link URL}s.
 * <p>
 * {@link #urlToFile(URL)} and {@link #fileToUrl(File)} and {@link #getParentUrl(URL)} are used to work with files across platforms
 */
public class URLs {

    /**
     * Are we running on Windows?
     */
    static final boolean IS_WINDOWS_OS = System.getProperty("os.name").toUpperCase()
            .contains("WINDOWS");

    /**
     * Changes the ending (e.g. ".sld") of a {@link URL}
     * 
     * @param url {@link URL} like <code>file:/sds/a.bmp</code> or <code>http://www.some.org/foo/bar.shp</code>
     * @param postfix New file extension for the {@link URL} without <code>.</code>
     * @return A new {@link URL} with new extension.
     * @throws {@link MalformedURLException} if the new {@link URL} can not be created.
     */
    public static URL changeUrlExt(URL url, String postfix) throws IllegalArgumentException {
        String a = url.toString();
        int lastDotPos = a.lastIndexOf('.');
        if (lastDotPos >= 0) {
            a = a.substring(0, lastDotPos);
        }
        a = a + "." + postfix;
        try {
            return new URL(a);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(
                    "Failed to create a new URL for " + url + " with new extension " + postfix, e);
        }
    }

    /**
     * Extends a {@link URL}.
     * 
     * @param base Has to be a {@link URL} pointing to a directory. If it doesn't end with a <code>/</code> it will be added automatically.
     * @param extension The part that will be added to the {@link URL}
     * @throws MalformedURLException if the new {@link URL} can not be created.
     */
    public static URL extendUrl(URL base, String extension) throws MalformedURLException {
        if (base == null) {
            throw new NullPointerException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1, "base"));
        }
        if (extension == null) {
            throw new NullPointerException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1, "extension"));
        }
        String a = base.toString();
        if (!a.endsWith("/")) {
            a += "/";
        }
        a += extension;
        return new URL(a);
    }

    /**
     * A replacement for File.toURI().toURL().
     * <p>
     * The handling of file.toURL() is broken; the handling of file.toURI().toURL() is known to be broken on a few platforms like mac. We have the
     * urlToFile( URL ) method that is able to untangle both these problems and we use it in the geotools library.
     * <p>
     * However occasionally we need to pick up a file and hand it to a third party library like EMF; this method performs a couple of sanity checks
     * which we can use to prepare a good URL reference to a file in these situtations.
     * 
     * @param file
     * @return URL
     */
    public static URL fileToUrl(File file) {
        try {
            URL url = file.toURI().toURL();
            String string = url.toString();
            if (string.contains("+")) {
                // this represents an invalid URL created using either
                // file.toURL(); or
                // file.toURI().toURL() on a specific version of Java 5 on Mac
                string = string.replace("+", "%2B");
            }
            if (string.contains(" ")) {
                // this represents an invalid URL created using either
                // file.toURL(); or
                // file.toURI().toURL() on a specific version of Java 5 on Mac
                string = string.replace(" ", "%20");
            }
            return new URL(string);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * The function is supposed to be equivalent to {@link File#getParent()}. The {@link URL} is converted to a String, truncated to the last / and
     * then recreated as a new URL.
     * 
     * @throws {@link MalformedURLException} if the parent {@link URL} can not be created.
     */
    public static URL getParentUrl(URL url) throws MalformedURLException {
        String a = url.toString();
        int lastDotPos = a.lastIndexOf('/');
        if (lastDotPos >= 0) {
            a = a.substring(0, lastDotPos);
        }
        // The parent of jar:file:some!/bar.file is jar:file:some!/, not jar:file:some!
        if (a.endsWith("!")) {
            a += "/";
        }
        return new URL(a);
    }

    /**
     * Takes a URL and converts it to a File. The attempts to deal with Windows UNC format specific problems, specifically files located on network
     * shares and different drives.
     * <p>
     * If the URL.getAuthority() returns null or is empty, then only the url's path property is used to construct the file. Otherwise, the authority
     * is prefixed before the path.
     * <p>
     * It is assumed that url.getProtocol returns "file".
     * <p>
     * Authority is the drive or network share the file is located on. Such as "C:", "E:", "\\fooServer"
     * 
     * @param url a URL object that uses protocol "file"
     * @return a File that corresponds to the URL's location
     */
    public static File urlToFile(URL url) {
        if (!"file".equals(url.getProtocol())) {
            return null; // not a File URL
        }
        String string = url.toString();
        if (url.getQuery() != null) {
            string = string.substring(0, string.indexOf("?"));
        }
        if (string.contains("+")) {
            // this represents an invalid URL created using either
            // file.toURL(); or
            // file.toURI().toURL() on a specific version of Java 5 on Mac
            string = string.replace("+", "%2B");
        }
        try {
            string = URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not decode the URL to UTF-8 format", e);
        }
        String path3;
        String simplePrefix = "file:/";
        String standardPrefix = "file://";
        if (IS_WINDOWS_OS && string.startsWith(standardPrefix)) {
            // win32: host/share reference. Keep the host slashes.
            path3 = string.substring(standardPrefix.length() - 2);
            File f = new File(path3);
            if (!f.exists()) {
                // Make path relative to be backwards compatible.
                path3 = path3.substring(2, path3.length());
            }
        } else if (string.startsWith(standardPrefix)) {
            path3 = string.substring(standardPrefix.length());
        } else if (string.startsWith(simplePrefix)) {
            path3 = string.substring(simplePrefix.length() - 1);
        } else {
            String auth = url.getAuthority();
            String path2 = url.getPath().replace("%20", " ");
            if (auth != null && !auth.equals("")) {
                path3 = "//" + auth + path2;
            } else {
                path3 = path2;
            }
        }
        return new File(path3);
    }

}
