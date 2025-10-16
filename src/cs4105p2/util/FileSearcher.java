package cs4105p2.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearcher {
    /**
     * Convenience method that accepts string paths instead of File objects.
     *
     * @param dir Directory path to search within
     * @param searchStr Search string to match against file paths
     * @return List of matching files
     */
    public static List<File> search(String dir, String searchStr) {
        return search(new File(dir), searchStr);
    }

    /**
     * Path-based file searching.
     * Uses the search directory as the root directory and enables path-based searching.
     *
     * @param dir Directory to search within
     * @param searchStr Search string to match against file paths
     * @return List of matching files
     */
    public static List<File> search(File dir, String searchStr) {
        File rootDir = dir;
        boolean isSearchByPath = true;    
        return search(dir, rootDir, searchStr, isSearchByPath);
    }

    /**
     * Searches for files matching the given search string within a directory.
     *
     * @param dir Search starting directory
     * @param rootDir Root directory of the shared file system
     * @param searchStr Search string to match against file paths or names
     * @param isSearchByPath If true, matches against full path; if false, matches only against filename
     * @return List of files matching the search criteria
     */
    public static List<File> search(File dir, File rootDir, String searchStr, boolean isSearchByPath) {
        List<File> results = new ArrayList<File>();
        if (!rootDir.isDirectory()) {
            System.out.println("FileSearcher.search: Invalid rootDir" + rootDir);
            return results;
        }

        searchFiles(dir, rootDir, searchStr, isSearchByPath, results);

        return results;
    }



    /**
     * Recursively searches through directories to find matching files.
     * Performs case-insensitive substring matching against either the full path
     * or just the filename based on the isSearchByPath parameter.
     *
     * @param dir Current directory being searched
     * @param rootDir Root directory of the file system
     * @param searchStr Search string to match
     * @param isSearchByPath Whether to match against full path or just filename
     * @param results List to collect matching files
     */
    static private void searchFiles(File dir,
                                    File rootDir,
                                    String searchStr,
                                    boolean isSearchByPath,
                                    List<File> results) 
    {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                searchFiles(file, rootDir, searchStr, isSearchByPath, results);
            } else {
                String absolutePath = file.getAbsolutePath();
                String logicalPath = logicalPathName(absolutePath, rootDir.toString());
                String target = isSearchByPath
                        ? logicalPath.toLowerCase()
                        : file.getName().toLowerCase();

                if (target.contains(searchStr.toLowerCase())) {
                    results.add(file);
                }
            }
        }
    }

    /**
     * Creates a logical path relative to the root directory.
     * Converts absolute filesystem paths to logical paths within the application.
     * 
     * @param pathName Absolute/canonical path of the file
     * @param rootPath Root directory path to make relative to
     * @return The logical path relative to root, starting with "/" if at root
     */
    static private String logicalPathName(String pathName, String rootPath) {
        String p = pathName.replace(rootPath, "");
        if (p == "")
            p = "/";
        return p;
    }
}
