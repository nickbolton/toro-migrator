/*
 * Copyright 2000-2001,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jetspeed.util;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Task to overwrite Properties: used for JRP, TRP and Torque.properties
 *
 * @author     <a href="mailto:taylor@apache.org">David Sean Taylor</a>
 * @author     <a href="mailto:epugh@upstate.com">Eric Pugh</a>
 * @created    January 29, 2003
 * @version    $Id: OverwriteProperties.java,v 1.4 2004/02/23 03:23:42 jford Exp $
 */
public class OverwriteProperties {
    /**  The file to merge properties into */
    protected File baseProperties;
    /**  The file to pull the properties from */
    protected File properties;
    /**  The directory to look in for include files */
    protected File includeRoot;

    /**  Description of the Field */
    public boolean verbose = false;

    /**  An array of all the properties */
    protected ArrayList baseArray = new ArrayList(1024);
    /**  An array of all the properties that will be removed */
    protected ArrayList removeArray = new ArrayList(128);
    /**  Description of the Field */
    protected HashMap baseMap = new HashMap();
    /**  What to use as a line seperator */
    protected String lineSeparator = System.getProperty("line.separator", "\r\n");


    /**
     *  Sets the file to merge properties into
     *
     * @param  baseProperties  The file path to merge properties into
     */
    public void setBaseProperties(File baseProperties)
    {
        this.baseProperties = baseProperties;
    }


    /**
     *  Sets the file to pull properties from
     *
     * @param  properties  The file path to the pull the merge properties from
     */
    public void setProperties(File properties)
    {
        this.properties = properties;
    }


    /**
     *  Sets the directory to look for includes in.
     *
     * @param  includeRoot  the directory to look in.
     */
    public void setIncludeRoot(File includeRoot)
    {
        this.includeRoot = includeRoot;
    }


    /**
     *  Sets whether to output extra debugging info
     *
     * @param  verbose  The new verbose value
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }


    /**
     *  Return the file to merge propertie into
     *
     * @return    The baseProperties value
     */
    public File getBaseProperties()
    {
        return baseProperties;
    }


    /**
     *  Gets the properties attribute of the OverwriteProperties object
     *
     * @return    The properties value
     */
    public File getProperties()
    {
        return properties;
    }


    /**
     *  Gets the includeRoot attribute of the OverwriteProperties object
     *
     * @return    The includeRoot value
     */
    public File getIncludeRoot()
    {
        return includeRoot;
    }


    /**
     *  Gets the verbose attribute of the OverwriteProperties object
     *
     * @return    The verbose value
     */
    public boolean getVerbose()
    {
        return verbose;
    }


    /**
     *  The main program for the OverwriteProperties class
     *
     * @param  args           The command line arguments
     * @exception  Exception  Description of the Exception
     */
    public static void main(String[] args)
        throws Exception
    {
        OverwriteProperties overwriteProperties = new OverwriteProperties();

        try
        {
            if (args.length < 3)
            {
                System.out.println("Usage: java OverwriteProperties c:/temp/File1.props c:/temp/File2.props c:/include-root/");
                System.out.println("Usage: File1 will be modified, new parameters from File 2 will be added,");
                System.out.println("Usage: and same parameters will be updated. The include-root is where include files are found.");
                throw new Exception("Incorrect number of arguments supplied");
            }
            overwriteProperties.setBaseProperties(new File(args[0]));
            overwriteProperties.setProperties(new File(args[1]));
            overwriteProperties.setIncludeRoot(new File(args[2]));

            overwriteProperties.execute();

        }
        catch (FileNotFoundException ex)
        {
            System.err.println(ex.getMessage());
        }
        catch (IOException ex)
        {
            System.err.println(ex.getMessage());
        }
        catch (SecurityException ex)
        {
            System.err.println(ex.getMessage());
        }
    }


    /**  Description of the Method */
    public void execute() throws FileNotFoundException, IOException, SecurityException
    {

            if (verbose)
            {
                System.out.println("Merging into file " + getBaseProperties() + " file " + getProperties());
            }

            if (!getBaseProperties().exists())
            {
              throw new FileNotFoundException("Could not find file:" + getBaseProperties());
            }

            if (!getProperties().exists())
            {
              throw new FileNotFoundException("Could not find file:" + getProperties());
            }

            if (!getIncludeRoot().exists() || !getIncludeRoot().isDirectory())
            {
              throw new FileNotFoundException("Could not find directory:" + getIncludeRoot());
            }

            BufferedReader reader = new BufferedReader(new FileReader(baseProperties));
            int index = 0;
            String key = null;
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                StringTokenizer tokenizer = new StringTokenizer(line, "=");
                baseArray.add(index, line);
                if (verbose)
                {
                    System.out.println("While reading baseArray[" + index + "] = " + line);
                }
                if (tokenizer.countTokens() >= 1 
                    && !line.startsWith("#") 
                    && !line.startsWith("include") 
                    && !line.startsWith("module.packages"))
                {
                    key = tokenizer.nextToken().trim();
                    if (key != null && key.length() > 0)
                    {
                        baseMap.put(key, new Integer(index));
                        if (verbose)
                        {
                            System.out.println("baseMap[" + key + "," + index + "]");
                        }
                    }
                }
                index++;
            }
            reader.close();
            if (verbose)
            {
                System.out.println("\nOverwrite with Delta\n");
            }

            readProperties(properties, index);

            boolean flags[] = removeProperties();

            baseArray.trimToSize();
            writeToFile(flags);


    }


    /**
     *  Description of the Method
     *
     * @param  flags                      Description of the Parameter
     * @exception  FileNotFoundException  Description of the Exception
     * @exception  IOException            Description of the Exception
     */
    public void writeToFile(boolean[] flags)
        throws FileNotFoundException, IOException
        {
        FileOutputStream writer = new FileOutputStream(baseProperties);
        writer.flush();
        for (int i = 0; i < baseArray.size(); i++)
        {
            if (true == flags[i])
            {
                if (verbose)
                {
                    System.out.println("Skipping property[" + i + "] = " + baseArray.get(i));
                }
                continue;
            }
            if (verbose)
            {
                System.out.println("Writing property[" + i + "] = " + baseArray.get(i));
            }
            writer.write(((String) baseArray.get(i)).getBytes());
            writer.write(lineSeparator.getBytes());
            writer.flush();
        }
        writer.close();

    }


    /**
     *  Description of the Method
     *
     * @return    Description of the Return Value
     */
    public boolean[] removeProperties()
    {

        boolean flags[] = new boolean[baseArray.size()];

        for (int i = 0; i < baseArray.size(); i++)
        {
            flags[i] = false;
        }
        for (int ix = 0; ix < removeArray.size(); ix++)
        {
            String prefix = (String) removeArray.get(ix);
            for (int iy = 0; iy < baseArray.size(); iy++)
            {
                String line = (String) baseArray.get(iy);
                if (line.startsWith(prefix))
                {
                    flags[iy] = true;
                    if (verbose)
                    {
                        System.out.println("flagging removal of property: " + line);
                    }
                }
            }
        }
        return flags;
    }


    /**
     *  Reads in the properties from the specified file
     *
     * @param  propFile                   Description of the Parameter
     * @param  index                      Description of the Parameter
     * @exception  FileNotFoundException  Description of the Exception
     * @exception  IOException            Description of the Exception
     */
    public void readProperties(File propFile, int index)
        throws FileNotFoundException, IOException
        {
        BufferedReader reader = new BufferedReader(new FileReader(propFile));
        String key = null;
        String line = null;

        while ((line = reader.readLine()) != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(line, "=");

            int count = tokenizer.countTokens();
            if (count == 2 && line.startsWith("include"))
            {
                key = tokenizer.nextToken().trim();
                File includeFile = new File(includeRoot + tokenizer.nextToken().trim());
                if (verbose)
                {
                  System.out.println("include File = " + includeFile);
                }
                readProperties(includeFile, index);
                continue;
            }
            if (count >= 1 && line.startsWith("module.packages"))
            {
                baseArray.add(index, line);
                if (verbose)
                {
                    System.out.println("Adding module.package to baseArray[" + index + "] = " + line);
                }
                index++;

                key = line.trim();
                if (baseMap.containsKey(key))
                {
                    int ix = ((Integer) baseMap.get(key)).intValue();
                    baseArray.set(ix, line);
                    if (verbose)
                    {
                        System.out.println("Resetting baseArray[" + ix + "] = " + line);
                    }
                }

                continue;
            }
            if (count >= 1 && line.startsWith("-"))
            {
                // remove from base

                String prefix = line.trim().substring(1);
                removeArray.add(prefix);
                if (verbose)
                {
                    System.out.println("Flagging for removal = " + line);
                }
                continue;
            }
            if (count >= 1 && !line.startsWith("#"))
            {
                key = tokenizer.nextToken().trim();
                if (key != null && key.length() > 0)
                {
                    if (baseMap.containsKey(key))
                    {
                        int ix = ((Integer) baseMap.get(key)).intValue();
                        baseArray.set(ix, line);
                        if (verbose)
                        {
                            System.out.println("Resetting baseArray[" + ix + "] = " + line);
                        }
                    }
                    else
                    {
                        baseArray.add(index, line);
                        if (verbose)
                        {
                            System.out.println("Adding new entry to baseArray[" + index + "] = " + line);
                        }
                        baseMap.put(key, new Integer(index));
                        if (verbose)
                        {
                            System.out.println("baseMap[" + key + "," + index + "]");
                        }
                        index++;
                    }
                }
            }

        }
        reader.close();

    }

}




