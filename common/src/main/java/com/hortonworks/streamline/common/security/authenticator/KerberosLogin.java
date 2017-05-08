/**
 * Copyright 2017 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.common.security.authenticator;

import com.hortonworks.streamline.common.util.Shell;
import com.hortonworks.streamline.common.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * This class is responsible for logging in to Kerberos and refreshing credentials for
 * login corresponding to a login context section in jaas config
 */
public class KerberosLogin extends AbstractLogin {
    private static final Logger log = LoggerFactory.getLogger(KerberosLogin.class);
    private static final Random RNG = new Random();

    public static final String KINIT_CMD = "kinit.cmd";
    public static final String TICKET_RENEW_WINDOW_FACTOR = "ticket.renew.window.factor";
    public static final String TICKET_RENEW_JITTER = "ticket.renew.jitter";
    public static final String MIN_TIME_BEFORE_RELOGIN = "min.time.before.relogin";

    private Thread t;
    private boolean isKrbTicket;
    private boolean isUsingTicketCache;
    private String principal;
    // LoginThread will sleep until 80% of time from last refresh to
    // ticket's expiry has been reached, at which time it will wake
    // and try to renew the ticket.
    private double ticketRenewWindowFactor = 0.8;
    /**
     * Percentage of random jitter added to the renewal time
     */
    private double ticketRenewJitter = 0.05;
    // Regardless of ticketRenewWindowFactor setting above, thread will not sleep between refresh
    // attempts any less than 1 minute (60*1000 milliseconds = 1 minute) unless it causes expiration.
    // Change the '1' to e.g. 5, to change this to 5 minutes.
    private long minTimeBeforeRelogin = 1 * 60 * 1000;
    private String kinitCmd = "/usr/bin/kinit";

    /**
     * Method to configure this instance with specific properties
     * @param loginContextName
     *               name of section in JAAS file that will be used to login.
     *               Passed as first param to javax.security.auth.login.LoginContext().
     * @param configs configure Login with the given key-value pairs.
     */
    public void configure(Map<String, ?> configs, final String loginContextName) {
        super.configure(configs, loginContextName);
        if (configs.get(TICKET_RENEW_WINDOW_FACTOR) != null) {
            this.ticketRenewWindowFactor = Double.parseDouble((String) configs.get(TICKET_RENEW_WINDOW_FACTOR));
        }
        if (configs.get(TICKET_RENEW_JITTER) != null) {
            this.ticketRenewJitter = Double.parseDouble((String) configs.get(TICKET_RENEW_JITTER));
        }
        if (configs.get(MIN_TIME_BEFORE_RELOGIN) != null) {
            this.minTimeBeforeRelogin = Long.parseLong((String) configs.get(MIN_TIME_BEFORE_RELOGIN));
        }
        if (configs.get(KINIT_CMD) != null) {
            this.kinitCmd = (String) configs.get(KINIT_CMD);
        }
    }

    /**
     * Method called once initially to login. It also starts the thread used
     * to periodically re-login to the Kerberos Authentication Server.
     * @return
     * @throws LoginException if login fails
     */
    @Override
    public LoginContext login() throws LoginException {
        super.login();
        isKrbTicket = !loginContext.getSubject().getPrivateCredentials(KerberosTicket.class).isEmpty();
        if (!isKrbTicket) {
            log.info("It is not a Kerberos ticket");
            t = null;
            // if no TGT, do not bother with ticket management.
            return loginContext;
        }
        log.info("It is a Kerberos ticket");
        AppConfigurationEntry[] entries = Configuration.getConfiguration().getAppConfigurationEntry(loginContextName);
        if (entries.length == 0) {
            isUsingTicketCache = false;
            principal = null;
        } else {
            // there will only be a single entry
            AppConfigurationEntry entry = entries[0];
            if (entry.getOptions().get("useTicketCache") != null) {
                String val = (String) entry.getOptions().get("useTicketCache");
                isUsingTicketCache = val.equals("true");
            } else
                isUsingTicketCache = false;
            if (entry.getOptions().get("principal") != null)
                principal = (String) entry.getOptions().get("principal");
            else
                principal = null;
        }
        KerberosTicket tgt = getTGT();
        if (tgt != null) {
            if (isUsingTicketCache && tgt.getRenewTill() != null && tgt.getRenewTill().getTime() < tgt.getEndTime().getTime()) {
                log.warn("The TGT cannot be renewed beyond the next expiry date: {}. This process will not be able to authenticate new clients after that " +
                         "time. Ask your system administrator to either increase the 'renew until' time by doing : 'modprinc -maxrenewlife {} ' within " +
                         "kadmin, or instead, to generate a keytab for {}. Because the TGT's expiry cannot be further extended by refreshing, exiting " +
                         "refresh thread now.", new Date(tgt.getEndTime().getTime()), principal, principal);
            } else {
                spawnReloginThread();
            }
        } else {
            log.warn("No tgt found for principal {}. Hence not spawning auto relogin thread.", principal);
        }
        return loginContext;
    }

    @Override
    public void close() {
        if ((t != null) && (t.isAlive())) {
            t.interrupt();
            try {
                t.join();
            } catch (InterruptedException e) {
                log.warn("Error while waiting for Login thread to shutdown: " + e, e);
            }
        }
    }

    private void spawnReloginThread () {
        // Refresh the Ticket Granting Ticket (TGT) periodically. How often to refresh is determined by the
        // TGT's existing expiry date and the configured minTimeBeforeRelogin. For testing and development,
        // you can decrease the interval of expiration of tickets (for example, to 3 minutes) by running:
        //  "modprinc -maxlife 3mins <principal>" in kadmin.
        t = Utils.newThread("kerberos-refresh-thread", new Runnable() {
            public void run() {
                log.info("TGT refresh thread started.");
                while (true) {  // renewal thread's main loop. if it exits from here, thread will exit.
                    KerberosTicket tgt = getTGT();
                    Date nextRefreshDate;
                    long now = System.currentTimeMillis();
                    long nextRefresh = getRefreshTime(tgt);
                    long expiry = tgt.getEndTime().getTime();
                    // determine how long to sleep from looking at ticket's expiry.
                    // We should not allow the ticket to expire, but we should take into consideration
                    // minTimeBeforeRelogin. Will not sleep less than minTimeBeforeRelogin, unless doing so
                    // would cause ticket expiration.
                    if ((now + minTimeBeforeRelogin) <= expiry) {
                        if (nextRefresh < (now + minTimeBeforeRelogin)) {
                            // next scheduled refresh is sooner than (now + MIN_TIME_BEFORE_LOGIN).
                            log.warn("TGT refresh thread time adjusted from {} to {} since the former is sooner " +
                                            "than the minimum refresh interval ({} seconds) from now.",
                                    new Date(nextRefresh), new Date(now + minTimeBeforeRelogin), minTimeBeforeRelogin / 1000);
                        }
                        nextRefresh = Math.max(nextRefresh, now + minTimeBeforeRelogin);
                    }
                    nextRefreshDate = new Date(nextRefresh);
                    if (nextRefresh > expiry) {
                        log.error("Next refresh: {} is later than expiry {}. This may indicate a clock skew problem." +
                                        "Check that this host and the KDC hosts' clocks are in sync. Exiting refresh thread.",
                                nextRefreshDate, new Date(expiry));
                        return;
                    }
                    if (now < nextRefresh) {
                        log.info("TGT refresh sleeping until: {}", nextRefreshDate);
                        try {
                            Thread.sleep(nextRefresh - now);
                        } catch (InterruptedException ie) {
                            log.warn("TGT renewal thread has been interrupted and will exit.");
                            return;
                        }
                    } else {
                        log.error("NextRefresh: {} is in the past: exiting refresh thread. Check"
                                + " clock sync between this host and KDC - (KDC's clock is likely ahead of this host)."
                                + " Manual intervention will be required for this client to successfully authenticate."
                                + " Exiting refresh thread.", nextRefreshDate);
                        return;
                    }
                    if (isUsingTicketCache) {
                        String kinitArgs = "-R";
                        try {
                            log.debug("Running ticket cache refresh command: {} {}", kinitCmd, kinitArgs);
                            Shell.execCommand(kinitCmd, kinitArgs);
                        } catch (Exception e) {
                                    log.warn("Could not renew TGT due to problem running shell command: '" + kinitCmd
                                            + " " + kinitArgs + "'" + "; exception was: " + e + ". Exiting refresh thread.", e);
                                    return;
                        }
                    }
                    try {
                        reLogin();
                    } catch (LoginException le) {
                        log.error("Failed to refresh TGT: refresh thread exiting now.", le);
                        return;
                    }
                }
            }
        }, true);
        t.start();
    }

    private long getRefreshTime(KerberosTicket tgt) {
        long start = tgt.getStartTime().getTime();
        long expires = tgt.getEndTime().getTime();
        log.info("TGT valid starting at: {}", tgt.getStartTime());
        log.info("TGT expires: {}", tgt.getEndTime());
        long proposedSleepInterval = (long) ((expires - start) * (ticketRenewWindowFactor + (ticketRenewJitter * RNG.nextDouble())));
        long proposedRefresh = start + proposedSleepInterval;
        if (proposedRefresh > expires)
            // proposedRefresh is too far in the future: it's after ticket expires: simply return now.
            return System.currentTimeMillis();
        else
            return proposedRefresh;
    }

    private KerberosTicket getTGT() {
        Set<KerberosTicket> tickets = loginContext.getSubject().getPrivateCredentials(KerberosTicket.class);
        for (KerberosTicket ticket : tickets) {
            KerberosPrincipal server = ticket.getServer();
            if (server.getName().equals("krbtgt/" + server.getRealm() + "@" + server.getRealm())) {
                log.debug("Found TGT with client principal '{}' and server principal '{}'.", ticket.getClient().getName(),
                        ticket.getServer().getName());
                return ticket;
            }
        }
        return null;
    }

    /**
     * Re-login a principal. This method assumes that {@link #login()} has happened already.
     * @throws javax.security.auth.login.LoginException on a failure
     */
    private void reLogin() throws LoginException {
        log.info("Initiating logout for {}", principal);
        //clear up the kerberos state. But the tokens are not cleared! As per
        //the Java kerberos login module code, only the kerberos credentials
        //are cleared
        loginContext.logout();
        //login and also update the subject field of the original LoginContext to
        //have the new credentials (pass it to the LoginContext constructor)
        loginContext = new LoginContext(loginContextName, loginContext.getSubject());
        log.info("Initiating re-login for {}", principal);
        loginContext.login();
        log.info("Successfully logged in from auto relogin thread");
    }
}
