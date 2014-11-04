/*
 * Copyright (C) 2014 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reaktEU.ewViewer.data;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public interface ProgressListener {

    void onProgress(int step, int total);
}
