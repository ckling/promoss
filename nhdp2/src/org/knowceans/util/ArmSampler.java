/*
 * Created on Jul 30, 2005
 */
/*
 * (C) Copyright 2005, Gregor Heinrich (gregor :: arbylon : net) (This file is
 * part of the knowceans-arms (org.knowceans.arms.*) experimental software package.)
 */
/*
 * knowceans-arms is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 */
/*
 * knowceans-arms is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.knowceans.util;

/**
 * ArmSampler implements an adaptive rejection Metropolis sampler (ARMS) that
 * can sample from virtually any univariate distribution. The method performs
 * best if a log-concave density is being sampled from, but it also works for
 * other densities, for which an additional Metropolis step is inserted. The
 * greater the difference to log-concave shape, the more Metropolis rejections
 * must be expected.
 * <p>
 * This implementation is a port of the original c / fortran implementation by
 * Wally Gilks available at
 * http://www.mrc-bsu.cam.ac.uk/BSUsite/Research/ars.shtml.
 * <p>
 * Please acknowledge this work by referencing the relevant scientific
 * literature and program code (Web: http://www.arbylon.net/projects).
 * <p>
 * References:
 * <p>
 * Gilks, W. R. (1992) Derivative-free adaptive rejection sampling for Gibbs
 * sampling. Bayesian Statistics 4, (eds. Bernardo, J., Berger, J., Dawid, A.
 * P., and Smith, A. F. M.) Oxford University Press.
 * <p>
 * Gilks, W. R., Best, N. G. and Tan, K. K. C. (1995) Adaptive rejection
 * Metropolis sampling. Applied Statistics, 44, 455-472.
 * <p>
 * Gilks, W. R. and Wild, P. (1992) Adaptive rejection sampling for Gibbs
 * sampling. Applied Statistics 41, pp 337-348.
 * 
 * @author gregor heinrich
 */
public abstract class ArmSampler {

    /**
     * init (nothing to do)
     */
    public ArmSampler() {

    }

    /**
     * Abstract function to implement the log pdf.
     * 
     * @param x
     * @param params
     * @return
     */
    public abstract double logpdf(double x, Object params);

    /* adaptive rejection metropolis sampling */

    Object params;

    /**
     * dereference for ported c pointers
     */
    public static final int DEREF = 0;

    /** a point in the x,y plane */
    class Point {
        /** x and y coordinates */
        double x, y;

        /** exp(y-ymax+YCEIL) */
        double ey;

        /** integral up to x of rejection envelope */
        double cum;

        /** is y an evaluated point of log-density */
        int f;

        /** envelope points to left and right of x */
        Point pl, pr;
    }

    /** attributes of the entire rejection envelope */
    class Envelope {
        /** number of POINTs in current envelope */
        int cpoint;

        /** max number of POINTs allowed in envelope */
        int npoint;

        /** number of function evaluations performed */
        int[] neval;

        /** the maximum y-value in the current envelope */
        double ymax;

        /** start of storage of envelope POINTs */
        Point[] p;

        /** adjustment for convexity */
        double[] convex;
    }

    /** for metropolis step */
    class Metropolis {
        /** whether metropolis is to be used */
        boolean on;

        /** previous Markov chain iterate */
        double xprev;

        /** current log density at xprev */
        double yprev;
    }

    /** critical relative x-value difference */
    public static final double XEPS = 0.00001;

    /** critical y-value difference */
    public static final double YEPS = 0.1;

    /** critical relative exp(y) difference */
    public static final double EYEPS = 0.001;

    /** maximum y avoiding overflow in exp(y) */
    public static final double YCEIL = 50.;

    /**
     * adaptive rejection metropolis sampling - simplified argument list
     * 
     * @param params[] parameters of the pdf to be sampled
     * @param ninit : number of starting values to be used
     * @param xl[] : left bound ([] for pointer)
     * @param xr[] : right bound ([] for pointer)
     * @param dometrop : whether metropolis step is required
     * @param xprev[] : current value from markov chain ([] for pointer)
     * @return : sampled value
     * @throws Exception
     */
    public double armsSimple(Object params, int ninit, double[] xl,
        double[] xr, boolean dometrop, double[] xprev) throws Exception {
        double[] xinit = new double[ninit];
        double[] convex = new double[] {1.0};
        double[] qcent = null, xcent = null;
        int i, npoint = 100, nsamp = 1, ncent = 0;
        int[] neval = {0};
        double[] xsamp = new double[1];

        /*
         * set up starting values
         */
        for (i = 0; i < ninit; i++) {
            xinit[i] = xl[DEREF] + (i + 1.0) * (xr[DEREF] - xl[DEREF])
                / (ninit + 1.0);
        }

        arms(params, xinit, ninit, xl, xr, convex, npoint, dometrop, xprev,
            xsamp, nsamp, qcent, xcent, ncent, neval);

        return xsamp[DEREF];
    }

    /**
     * to perform derivative-free adaptive rejection sampling with metropolis
     * step
     * 
     * @param params parameters of the pdf to be sampled
     * @param xinit[] : starting values for x in ascending order
     * @param ninit : number of starting values supplied
     * @param xl[] : left bound ([] for pointer)
     * @param xr[] : right bound ([] for pointer)
     * @param convex[] : adjustment for convexity ([] for pointer)
     * @param npoint : maximum number of envelope points
     * @param dometrop : whether metropolis step is required
     * @param xprev[] : previous value from markov chain ([] for pointer)
     * @param xsamp[] : to store sampled values
     * @param nsamp : number of sampled values to be obtained
     * @param qcent[] : percentages for envelope centiles
     * @param xcent[] : to store requested centiles
     * @param ncent : number of centiles requested
     * @param neval[] : on exit, the number of function evaluations performed
     *        ([] for pointer)
     * @return xsamp[] : sampled values
     * @throws Exception
     */
    public double[] arms(Object params, double[] xinit, int ninit, double[] xl,
        double[] xr, double[] convex, int npoint, boolean dometrop,
        double[] xprev, double[] xsamp, int nsamp, double[] qcent,
        double[] xcent, int ncent, int[] neval) throws Exception {

        /* set parameters for densities */
        this.params = params;

        /* rejection envelope */
        Envelope env;
        /* a working point, not yet incorporated in envelope */
        Point pwork = new Point();
        /* the number of x-values currently sampled */
        int msamp = 0;
        /* to hold bits for metropolis step */
        Metropolis metrop;
        int i, err;

        /*
         * check requested envelope centiles
         */
        for (i = 0; i < ncent; i++) {
            if ((qcent[i] < 0.0) || (qcent[i] > 100.0)) {
                throw new Exception(
                    "percentage requesting centile is out of range");
            }
        }

        env = new Envelope();

        /*
         * start setting up metropolis struct
         */
        metrop = new Metropolis();

        metrop.on = dometrop;

        /*
         * set up initial envelope
         */
        initial(xinit, ninit, xl[DEREF], xr[DEREF], npoint, env, convex, neval,
            metrop);
        /*
         * finish setting up metropolis struct (can only do this after setting
         * up env)
         */
        if (metrop.on) {
            if ((xprev[DEREF] < xl[DEREF]) || (xprev[DEREF] > xr[DEREF])) {
                throw new Exception(
                    "previous markov chain iterate out of range");
            }
            metrop.xprev = xprev[DEREF];
            metrop.yprev = perfunc(env, xprev[DEREF]);
        }

        /*
         * now do adaptive rejection
         */
        do {
            /*
             * sample a new point
             */
            sample(env, pwork);

            /*
             * perform rejection (and perhaps metropolis) tests
             */
            i = test(env, pwork, metrop);
            if (i == 1) {
                /*
                 * point accepted
                 */
                xsamp[msamp++] = pwork.x;
            } else if (i != 0) {
                throw new Exception(
                    "envelope error - violation without metropolis");
            }
        } while (msamp < nsamp);

        /*
         * nsamp points now sampled calculate requested envelope centiles
         */
        for (i = 0; i < ncent; i++) {
            invert(qcent[i] / 100.0, env, pwork);
            xcent[i] = pwork.x;
        }

        return xsamp;
    }

    /**
     * to set up initial envelope
     * 
     * @param xinit : initial x-values
     * @param ninit : number of initial x-values
     * @param xl,xr : lower and upper x-bounds
     * @param npoint : maximum number of POINTs allowed in envelope
     * @param env[] : rejection envelope attributes
     * @param convex[] : adjustment for convexity ([] for pointer)
     * @param neval[] : current number of function evaluations ([] for pointer)
     * @param metrop : for metropolis step
     * @throws Exception
     */
    int initial(double[] xinit, int ninit, double xl, double xr, int npoint,
        Envelope env, double[] convex, int[] neval, Metropolis metrop)
        throws Exception {
        int i, j, k, mpoint;
        Point[] q;

        if (ninit < 3) {
            throw new Exception("too few initial points");
        }

        mpoint = 2 * ninit + 1;
        if (npoint < mpoint) {
            throw new Exception("too many initial points");
        }

        if ((xinit[DEREF] <= xl) || (xinit[ninit - 1] >= xr)) {
            throw new Exception("initial points do not satisfy bounds ");
        }

        for (i = 1; i < ninit; i++) {
            if (xinit[i] <= xinit[i - 1]) {

                throw new Exception("data not ordered");
            }
        }

        if (convex[DEREF] < 0.0) {
            throw new Exception("negative convexity parameter");
        }

        /*
         * copy convexity address to env
         */
        env.convex = convex;

        /*
         * copy address for current number of function evaluations
         */
        env.neval = neval;
        /*
         * initialise current number of function evaluations
         */
        env.neval[DEREF] = 0;

        /*
         * set up space for envelope POINTs
         */
        env.npoint = npoint;

        env.p = new Point[npoint];

        /*
         * set up envelope POINTs
         */
        q = env.p;
        /*
         * left bound
         */
        q[DEREF] = new Point();
        q[DEREF].x = xl;
        q[DEREF].f = 0;
        q[DEREF].pl = null;
        q[DEREF].pr = q[1];
        for (j = 1, k = 0; j < mpoint - 1; j++) {
            q[j] = new Point();
            if ((j % 2) != 0) {
                /*
                 * point on log density
                 */
                q[j].x = xinit[k++];
                q[j].y = perfunc(env, q[j].x);
                q[j].f = 1;
            } else {
                /*
                 * intersection point
                 */
                q[j].f = 0;
            }
            q[j].pl = q[j - 1];
            q[j - 1].pr = q[j];
        }
        /*
         * right bound
         */
        q[j] = new Point();
        q[j].x = xr;
        q[j].f = 0;
        q[j].pl = q[j - 1];
        q[j - 1].pr = q[j];
        q[j].pr = null;

        /*
         * calculate intersection points
         */
        q = env.p;
        // ,,,
        for (j = 0; j < mpoint; j = j + 2) {
            meet(q[j], env, metrop);
        }

        /*
         * exponentiate and integrate envelope
         */
        cumulate(env);

        /*
         * note number of POINTs currently in envelope
         */
        env.cpoint = mpoint;

        return 0;
    }

    /**
     * To sample from piecewise exponential envelope
     * 
     * @param env : envelope attributes
     * @param p : a working POINT to hold the sampled value
     * @throws Exception
     */
    void sample(Envelope env, Point p) throws Exception {
        double prob;

        /*
         * sample a uniform
         */
        prob = Math.random();
        /*
         * get x-value correponding to a cumulative probability prob
         */
        invert(prob, env, p);

        return;
    }

    /**
     * to obtain a point corresponding to a qiven cumulative probability
     * 
     * @param prob : cumulative probability under envelope
     * @param env : envelope attributes
     * @param p : a working POINT to hold the sampled value
     * @throws Exception
     */
    void invert(double prob, Envelope env, Point p) throws Exception {
        double u, xl = 0, xr = 0, yl, yr, eyl, eyr, prop, z;
        Point q;

        /*
         * find rightmost point in envelope
         */
        q = env.p[DEREF];
        while (q.pr != null)
            q = q.pr;

        /*
         * find exponential piece containing point implied by prob
         */
        u = prob * q.cum;
        while (q.pl.cum > u)
            q = q.pl;

        /*
         * piece found: set left and right POINTs of p, etc.
         */
        p.pl = q.pl;
        p.pr = q;
        p.f = 0;
        p.cum = u;

        /*
         * calculate proportion of way through integral within this piece
         */
        prop = (u - q.pl.cum) / (q.cum - q.pl.cum);

        /*
         * get the required x-value
         */
        if (q.pl.x == q.x) {
            /*
             * interval is of zero length
             */
            p.x = q.x;
            p.y = q.y;
            p.ey = q.ey;
        } else {
            xl = q.pl.x;
            xr = q.x;
            yl = q.pl.y;
            yr = q.y;
            eyl = q.pl.ey;
            eyr = q.ey;
            if (Math.abs(yr - yl) < YEPS) {
                /*
                 * linear approximation was used in integration in function
                 * cumulate
                 */
                if (Math.abs(eyr - eyl) > EYEPS * Math.abs(eyr + eyl)) {
                    p.x = xl
                        + ((xr - xl) / (eyr - eyl))
                        * (-eyl + Math.sqrt((1. - prop) * eyl * eyl + prop
                            * eyr * eyr));
                } else {
                    p.x = xl + (xr - xl) * prop;
                }
                p.ey = ((p.x - xl) / (xr - xl)) * (eyr - eyl) + eyl;
                p.y = logshift(p.ey, env.ymax);
            } else {
                /*
                 * piece was integrated exactly in function cumulate
                 */
                p.x = xl
                    + ((xr - xl) / (yr - yl))
                    * (-yl + logshift(((1. - prop) * eyl + prop * eyr),
                        env.ymax));
                p.y = ((p.x - xl) / (xr - xl)) * (yr - yl) + yl;
                p.ey = expshift(p.y, env.ymax);
            }
        }

        /*
         * guard against imprecision yielding point outside interval
         */
        if ((p.x < xl) || (p.x > xr))
            throw new Exception("imprecision yielding point outside interval ");

        return;
    }

    /**
     * to perform rejection, squeezing, and metropolis tests
     * 
     * @param env : envelope attributes
     * @param p : point to be tested
     * @param metrop : data required for metropolis step
     * @throws Exception
     */
    int test(Envelope env, Point p, Metropolis metrop) throws Exception {
        double u, y, ysqueez, ynew, yold, znew, zold, w;
        Point ql, qr;

        /*
         * for rejection test
         */
        u = Math.random() * p.ey;
        y = logshift(u, env.ymax);

        if (!(metrop.on) && (p.pl.pl != null) && (p.pr.pr != null)) {
            /*
             * perform squeezing test
             */
            if (p.pl.f != 0) {
                ql = p.pl;
            } else {
                ql = p.pl.pl;
            }
            if (p.pr.f != 0) {
                qr = p.pr;
            } else {
                qr = p.pr.pr;
            }
            ysqueez = (qr.y * (p.x - ql.x) + ql.y * (qr.x - p.x))
                / (qr.x - ql.x);
            if (y <= ysqueez) {
                /*
                 * accept point at squeezing step
                 */
                return 1;
            }
        }

        /*
         * evaluate log density at point to be tested
         */
        ynew = perfunc(env, p.x);

        /*
         * perform rejection test
         */
        if (!(metrop.on) || ((metrop.on) && (y >= ynew))) {
            /*
             * update envelope
             */
            p.y = ynew;
            p.ey = expshift(p.y, env.ymax);
            p.f = 1;
            update(env, p, metrop);
            /*
             * perform rejection test
             */
            if (y >= ynew) {
                /*
                 * reject point at rejection step
                 */
                return 0;
            } else {
                /*
                 * accept point at rejection step
                 */
                return 1;
            }
        }

        /*
         * continue with metropolis step
         */
        yold = metrop.yprev;
        /*
         * find envelope piece containing metrop.xprev
         */
        ql = env.p[DEREF];
        while (ql.pl != null)
            ql = ql.pl;
        while (ql.pr.x < metrop.xprev)
            ql = ql.pr;
        qr = ql.pr;
        /*
         * calculate height of envelope at metrop.xprev
         */
        w = (metrop.xprev - ql.x) / (qr.x - ql.x);
        zold = ql.y + w * (qr.y - ql.y);
        znew = p.y;
        if (yold < zold)
            zold = yold;
        if (ynew < znew)
            znew = ynew;
        w = ynew - znew - yold + zold;
        if (w > 0.0)
            w = 0.0;

        if (w > -YCEIL) {
            w = Math.exp(w);
        } else {
            w = 0.0;
        }
        u = Math.random();
        if (u > w) {
            /*
             * metropolis says dont move, so replace current point with previous
             */
            /*
             * markov chain iterate
             */
            p.x = metrop.xprev;
            p.y = metrop.yprev;
            p.ey = expshift(p.y, env.ymax);
            p.f = 1;
            p.pl = ql;
            p.pr = qr;
        } else {
            /*
             * trial point accepted by metropolis, so update previous markov
             */
            /*
             * chain iterate
             */
            metrop.xprev = p.x;
            metrop.yprev = ynew;
        }
        return 1;
    }

    /**
     * to update envelope to incorporate new point on log density
     * 
     * @param env : envelope attributes
     * @param p : point to be incorporated
     * @param metrop : for metropolis step
     * @throws Exception
     */
    int update(Envelope env, Point p, Metropolis metrop) throws Exception {
        Point m, ql, qr, q;

        if ((p.f == 0) || (env.cpoint > env.npoint - 2)) {
            /*
             * y-value has not been evaluated or no room for further points
             */
            /*
             * ignore this point
             */
            return 0;
        }

        /*
         * copy working POINT p to a new POINT q
         */
        q = new Point();
        q.x = p.x;
        q.y = p.y;
        q.f = 1;

        /*
         * allocate an unused POINT for a new intersection
         */
        m = new Point();
        m.f = 0;
        if ((p.pl.f != 0) && (p.pr.f == 0)) {
            /*
             * left end of piece is on log density; right end is not set up new
             * intersection in interval between p.pl and p
             */
            m.pl = p.pl;
            m.pr = q;
            q.pl = m;
            q.pr = p.pr;
            m.pl.pr = m;
            q.pr.pl = q;
        } else if ((p.pl.f == 0) && (p.pr.f != 0)) {
            /*
             * left end of interval is not on log density; right end is set up
             * new intersection in interval between p and p.pr
             */
            m.pr = p.pr;
            m.pl = q;
            q.pr = m;
            q.pl = p.pl;
            m.pr.pl = m;
            q.pl.pr = q;
        } else {
            /*
             * this should be impossible
             */
            throw new Exception("unknown error");
        }

        /*
         * now adjust position of q within interval if too close to an endpoint
         */
        if (q.pl.pl != null) {
            ql = q.pl.pl;
        } else {
            ql = q.pl;
        }
        if (q.pr.pr != null) {
            qr = q.pr.pr;
        } else {
            qr = q.pr;
        }
        if (q.x < (1. - XEPS) * ql.x + XEPS * qr.x) {
            /*
             * q too close to left end of interval
             */
            q.x = (1. - XEPS) * ql.x + XEPS * qr.x;
            q.y = perfunc(env, q.x);
        } else if (q.x > XEPS * ql.x + (1. - XEPS) * qr.x) {
            /*
             * q too close to right end of interval
             */
            q.x = XEPS * ql.x + (1. - XEPS) * qr.x;
            q.y = perfunc(env, q.x);
        }

        /*
         * revise intersection points
         */
        meet(q.pl, env, metrop);
        meet(q.pr, env, metrop);
        if (q.pl.pl != null) {
            meet(q.pl.pl.pl, env, metrop);
        }
        if (q.pr.pr != null) {
            meet(q.pr.pr.pr, env, metrop);
        }

        /*
         * exponentiate and integrate new envelope
         */
        cumulate(env);

        return 0;
    }

    /**
     * to exponentiate and integrate envelope
     * 
     * @param env : envelope attributes
     * @throws Exception
     */
    void cumulate(Envelope env) throws Exception {
        Point q, qlmost;

        qlmost = env.p[DEREF];
        /*
         * find left end of envelope
         */
        while (qlmost.pl != null)
            qlmost = qlmost.pl;

        /*
         * find maximum y-value: search envelope
         */
        env.ymax = qlmost.y;
        for (q = qlmost.pr; q != null; q = q.pr) {
            if (q.y > env.ymax)
                env.ymax = q.y;
        }

        /*
         * exponentiate envelope
         */
        for (q = qlmost; q != null; q = q.pr) {
            q.ey = expshift(q.y, env.ymax);
        }

        /*
         * integrate exponentiated envelope
         */
        qlmost.cum = 0.;
        for (q = qlmost.pr; q != null; q = q.pr) {
            q.cum = q.pl.cum + area(q);
        }

        return;
    }

    /**
     * To find where two chords intersect
     * 
     * @param q : to store point of intersection
     * @param env : envelope attributes
     * @param metrop : for metropolis step
     * @throws Exception
     */
    int meet(Point q, Envelope env, Metropolis metrop) throws Exception {
        double gl = 0, gr = 0, grl = 0, dl = 0, dr = 0;
        int il, ir, irl;

        if (q.f != 0) {
            /*
             * this is not an intersection point
             */
            throw new Exception("this is not an intersection point");
        }

        /*
         * calculate coordinates of point of intersection
         */
        if ((q.pl != null) && (q.pl.pl.pl != null)) {
            /*
             * chord gradient can be calculated at left end of interval
             */
            gl = (q.pl.y - q.pl.pl.pl.y) / (q.pl.x - q.pl.pl.pl.x);
            il = 1;
        } else {
            /*
             * no chord gradient on left
             */
            il = 0;
        }
        if ((q.pr != null) && (q.pr.pr.pr != null)) {
            /*
             * chord gradient can be calculated at right end of interval
             */
            gr = (q.pr.y - q.pr.pr.pr.y) / (q.pr.x - q.pr.pr.pr.x);
            ir = 1;
        } else {
            /*
             * no chord gradient on right
             */
            ir = 0;
        }
        if ((q.pl != null) && (q.pr != null)) {
            /*
             * chord gradient can be calculated across interval
             */
            grl = (q.pr.y - q.pl.y) / (q.pr.x - q.pl.x);
            irl = 1;
        } else {
            irl = 0;
        }

        if ((irl != 0) && (il != 0) && (gl < grl)) {
            /*
             * convexity on left exceeds current threshold
             */
            if (!(metrop.on)) {
                throw new Exception(
                    "convex on left: envelope violation without metropolis ");
            }
            /*
             * adjust left gradient
             */
            gl = gl + (1.0 + env.convex[DEREF]) * (grl - gl);
        }

        if ((irl != 0) && (ir != 0) && (gr > grl)) {
            /*
             * convexity on right exceeds current threshold
             */
            if (!(metrop.on)) {
                throw new Exception(
                    "convex on right: envelope violation without metropolis ");
            }
            /*
             * adjust right gradient
             */
            gr = gr + (1.0 + env.convex[DEREF]) * (grl - gr);
        }

        if ((il != 0) && (irl != 0)) {
            dr = (gl - grl) * (q.pr.x - q.pl.x);
            if (dr < YEPS) {
                /*
                 * adjust dr to avoid numerical problems
                 */
                dr = YEPS;
            }
        }

        if ((ir != 0) && (irl != 0)) {
            dl = (grl - gr) * (q.pr.x - q.pl.x);
            if (dl < YEPS) {
                /*
                 * adjust dl to avoid numerical problems
                 */
                dl = YEPS;
            }
        }

        if ((il != 0) && (ir != 0) && (irl != 0)) {
            /*
             * gradients on both sides
             */
            q.x = (dl * q.pr.x + dr * q.pl.x) / (dl + dr);
            q.y = (dl * q.pr.y + dr * q.pl.y + dl * dr) / (dl + dr);
        } else if ((il != 0) && (irl != 0)) {
            /*
             * gradient only on left side, but not right hand bound
             */
            q.x = q.pr.x;
            q.y = q.pr.y + dr;
        } else if ((ir != 0) && (irl != 0)) {
            /*
             * gradient only on right side, but not left hand bound
             */
            q.x = q.pl.x;
            q.y = q.pl.y + dl;
        } else if (il != 0) {
            /*
             * right hand bound
             */
            q.y = q.pl.y + gl * (q.x - q.pl.x);
        } else if (ir != 0) {
            /*
             * left hand bound
             */
            q.y = q.pr.y - gr * (q.pr.x - q.x);
        } else {
            throw new Exception(
                "gradient on neither side - should be impossible ");
        }
        if (((q.pl != null) && (q.x < q.pl.x))
            || ((q.pr != null) && (q.x > q.pr.x))) {
            throw new Exception(
                "intersection point outside interval (through imprecision)");
        }
        /*
         * successful exit : intersection has been calculated
         */
        return 0;
    }

    /**
     * To integrate piece of exponentiated envelope to left of POINT q
     * 
     * @param Point q
     * @throws Exception
     */
    double area(Point q) throws Exception {
        double a;

        if (q.pl == null) {
            /*
             * this is leftmost point in envelope
             */
            throw new Exception("this is leftmost point in envelope");
        } else if (q.pl.x == q.x) {
            /*
             * interval is zero length
             */
            a = 0.;
        } else if (Math.abs(q.y - q.pl.y) < YEPS) {
            /*
             * integrate straight line piece
             */
            a = 0.5 * (q.ey + q.pl.ey) * (q.x - q.pl.x);
        } else {
            /*
             * integrate exponential piece
             */
            a = ((q.ey - q.pl.ey) / (q.y - q.pl.y)) * (q.x - q.pl.x);
        }
        return a;
    }

    /**
     * to exponentiate shifted y without underflow
     */
    double expshift(double y, double y0) {
        if (y - y0 > -2.0 * YCEIL) {
            return Math.exp(y - y0 + YCEIL);
        } else {
            return 0.0;
        }
    }

    /**
     * inverse of function expshift
     */
    double logshift(double y, double y0) {
        return (Math.log(y) + y0 - YCEIL);
    }

    /**
     * to evaluate log density and increment count of evaluations
     * 
     * @param *lpdf : structure containing pointers to log-density function and
     *        data
     * @param *env : envelope attributes
     * @param x : point at which to evaluate log density
     */
    double perfunc(Envelope env, double x) {
        double y;

        /*
         * evaluate density function
         */
        y = logpdf(x, params);

        /*
         * increment count of function evaluations
         */
        env.neval[DEREF]++;

        return y;
    }
}
