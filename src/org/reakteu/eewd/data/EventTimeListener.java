/*
 * Copyright (C) 2014-2015 by gempa GmbH - http://gempa.de
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */
package org.reakteu.eewd.data;

/**
 *
 * @author Stephan Herrnkind herrnkind at gempa dot de
 */
public interface EventTimeListener {

    void processEventTime(EventData event, Long originTimeOffset);

}
