/**
 * MIT license
 * Copyright 2017 Autodesk, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.jenkinsci.plugins.benchmark.utilities;

import hudson.model.AbstractBuild;
import org.jenkinsci.plugins.benchmark.parsers.MapperBase;

import java.io.File;
import java.util.logging.Logger;

/**
 * Parallel runnable
 *
 * @author Daniel Mercier
 * @since 6/20/2017.
 */
public class RunnableReader implements Runnable {

    private final AbstractBuild startBuild;
    private final AbstractBuild endBuild;
    private final MapperBase mapper;

    public RunnableReader(AbstractBuild startBuild, AbstractBuild endBuild, MapperBase mapper) {
        this.startBuild = startBuild;
        this.endBuild = endBuild;
        this.mapper = mapper;
    }
    private static final Logger log = Logger.getLogger(RunnableReader.class.getName());

    @SuppressWarnings("unused")
    @Override
    public void run() {
        AbstractBuild build = this.startBuild;
        try {
            while (build != null && build != endBuild) {
                StringBuffer rawFilename = new StringBuffer();
                rawFilename.append(build.getRootDir().getAbsolutePath());
                rawFilename.append(File.separator);
                rawFilename.append("BenchmarkResult.json");
                this.mapper.importFromFile(rawFilename.toString());
                build = build.getPreviousBuild();
            }
        } catch (Exception e){
            log.warning("Warning:" + Thread.currentThread().getName() + "- Build:" + build.getNumber()+ " - " + e.getCause());
        }
    }

}
