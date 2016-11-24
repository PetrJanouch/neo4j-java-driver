/**
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 * <p>
 * This file is part of Neo4j.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.v1.util.cc;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static java.lang.System.lineSeparator;

final class SharedCluster
{
    private static Cluster clusterInstance;

    private SharedCluster()
    {
    }

    static Cluster get()
    {
        assertClusterExists();
        return clusterInstance;
    }

    static boolean exists()
    {
        return clusterInstance != null;
    }

    static void install( String neo4jVersion, int cores, int readReplicas, String password, Path path )
    {
        assertClusterDoesNotExist();
        ClusterControl.installCluster( neo4jVersion, cores, readReplicas, password, path );
        clusterInstance = new Cluster( path, password );
    }

    static void start()
    {
        assertClusterExists();
        String output = ClusterControl.startCluster( clusterInstance.getPath() );
        Set<ClusterMember> members = parseStartCommandOutput( output );
        clusterInstance = clusterInstance.withMembers( members );
    }

    static void stop()
    {
        assertClusterExists();
        ClusterControl.stopCluster( clusterInstance.getPath() );
    }

    static void kill()
    {
        assertClusterExists();
        ClusterControl.killCluster( clusterInstance.getPath() );
    }

    private static Set<ClusterMember> parseStartCommandOutput( String output )
    {
        Set<ClusterMember> result = new HashSet<>();

        String[] lines = output.split( lineSeparator() );
        for ( String line : lines )
        {
            String[] clusterMemberSplit = line.split( " " );
            if ( clusterMemberSplit.length != 3 )
            {
                throw new IllegalArgumentException(
                        "Wrong start command output. " +
                        "Expected to have 'http_uri bolt_uri path' in line '" + line + "'" );
            }

            URI boltUri = URI.create( clusterMemberSplit[1] );
            Path path = Paths.get( clusterMemberSplit[1] );

            result.add( new ClusterMember( boltUri, path ) );
        }

        if ( result.isEmpty() )
        {
            throw new IllegalStateException( "No cluster members" );
        }

        return result;
    }

    private static void assertClusterExists()
    {
        if ( clusterInstance == null )
        {
            throw new IllegalStateException( "Shared cluster does not exist" );
        }
    }

    private static void assertClusterDoesNotExist()
    {
        if ( clusterInstance != null )
        {
            throw new IllegalStateException( "Shared cluster already exists" );
        }
    }
}
