/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.google.code.play;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Run Play! Server synchronously. Base class for "run" and "test" mojos.
 * 
 * @author <a href="mailto:gslowikowski@gmail.com">Grzegorz Slowikowski</a>
 */
public abstract class AbstractPlayRunMojo
    extends AbstractPlayServerMojo
{
    /**
     * ...
     * 
     * @parameter expression="${play.runFork}" default-value="true"
     * @since 1.0.0
     */
    private boolean runFork;

    abstract protected String getPlayId();

    @Override
    protected void internalExecute()
        throws MojoExecutionException, MojoFailureException, IOException
    {
        String playId = getPlayId();
        
        File baseDir = project.getBasedir();

        File pidFile = new File( baseDir, "server.pid" );
        if ( pidFile.exists() )
        {
            throw new MojoExecutionException( String.format( "Play! Server already started (\"%s\" file found)",
                                                             pidFile.getName() ) );
        }

        ConfigurationParser configParser =  getConfiguration( playId );

        Java javaTask = prepareAntJavaTask( configParser, playId, runFork );
        javaTask.setFailonerror( true );

        JavaRunnable runner = new JavaRunnable( javaTask );
        Thread t = new Thread( runner, "Play! Server runner" );
        getLog().info( "Launching Play! Server" );
        t.start();
        try
        {
            t.join(); // waiting for Ctrl+C if forked, joins immediately if not forking
        }
        catch ( InterruptedException e )
        {
            throw new MojoExecutionException( "?", e );
        }
        Exception runException = runner.getException();
        if ( runException != null )
        {
            throw new MojoExecutionException( "?", runException );
        }
        
        if ( !runFork )
        {
            while ( true ) // wait for Ctrl+C
            {
                try
                {
                    Thread.sleep( 10000 );
                }
                catch ( InterruptedException e )
                {
                    throw new MojoExecutionException( "?", e );
                }
            }
        }
    }

}
