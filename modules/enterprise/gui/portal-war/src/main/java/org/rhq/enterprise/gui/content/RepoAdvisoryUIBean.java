/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.content;

import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Pradeep Kilambi
 */
public class RepoAdvisoryUIBean extends PagedDataTableUIBean {
    public static final String MANAGED_BEAN_NAME = "RepoAdvisoryUIBean";
    private final Log log = LogFactory.getLog(RepoAdvisoryUIBean.class);

    public RepoAdvisoryUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new RepoAdvisoryDataModel(PageControlView.RepoAdvisoryList, MANAGED_BEAN_NAME);
        }
        return dataModel;

    }

    private class RepoAdvisoryDataModel extends PagedListDataModel<Advisory> {
        public RepoAdvisoryDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<Advisory> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));
            RepoManagerLocal manager = LookupUtil.getRepoManagerLocal();
            PageList<Advisory> results = manager.findAssociatedAdvisory(subject, id, pc);
            log.debug("Resulsts for advisory" + results);
            return results;
        }
    }

}
