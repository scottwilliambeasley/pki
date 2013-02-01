package com.netscape.ca;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.netscape.certsrv.acls.ACLInterceptor;
import com.netscape.certsrv.apps.CMS;
import com.netscape.certsrv.authentication.AuthMethodInterceptor;
import com.netscape.certsrv.base.EBaseException;
import com.netscape.certsrv.base.IConfigStore;
import com.netscape.certsrv.base.PKIException;
import com.netscape.cms.servlet.account.AccountService;
import com.netscape.cms.servlet.admin.GroupMemberService;
import com.netscape.cms.servlet.admin.GroupService;
import com.netscape.cms.servlet.admin.KRAConnectorService;
import com.netscape.cms.servlet.admin.SystemCertService;
import com.netscape.cms.servlet.admin.UserCertService;
import com.netscape.cms.servlet.admin.UserMembershipService;
import com.netscape.cms.servlet.admin.UserService;
import com.netscape.cms.servlet.cert.CertService;
import com.netscape.cms.servlet.csadmin.SecurityDomainService;
import com.netscape.cms.servlet.csadmin.SystemConfigService;
import com.netscape.cms.servlet.profile.ProfileService;
import com.netscape.cms.servlet.request.CertRequestService;

public class CertificateAuthorityApplication extends Application {
    private Set<Object> singletons = new HashSet<Object>();
    private Set<Class<?>> classes = new HashSet<Class<?>>();

    public CertificateAuthorityApplication() {

        // account
        classes.add(AccountService.class);

        // installer
        classes.add(SystemConfigService.class);

        // certs and requests
        classes.add(CertService.class);
        classes.add(CertRequestService.class);

        // profile management
        classes.add(ProfileService.class);

        // user and group management
        classes.add(GroupMemberService.class);
        classes.add(GroupService.class);
        classes.add(UserCertService.class);
        classes.add(UserMembershipService.class);
        classes.add(UserService.class);

        // system certs
        classes.add(SystemCertService.class);

        // kra connector
        classes.add(KRAConnectorService.class);

        // security domain
        IConfigStore cs = CMS.getConfigStore();

        // check server state
        int state;
        try {
            state = cs.getInteger("cs.state");
        } catch (EBaseException e) {
            CMS.debug(e);
            throw new RuntimeException(e);
        }

        // if server is configured, check security domain selection
        if (state == 1) {
            String select;
            try {
                select = cs.getString("securitydomain.select");
            } catch (EBaseException e) {
                CMS.debug(e);
                throw new RuntimeException(e);
            }

            // if it's a new security domain, register the service
            if ("new".equals(select)) {
                classes.add(SecurityDomainService.class);
            }
        }

        // exception mapper
        classes.add(PKIException.Mapper.class);

        // interceptors
        singletons.add(new AuthMethodInterceptor());
        singletons.add(new ACLInterceptor());
    }

    public Set<Class<?>> getClasses() {
        return classes;
    }

    public Set<Object> getSingletons() {
        return singletons;
    }

}
