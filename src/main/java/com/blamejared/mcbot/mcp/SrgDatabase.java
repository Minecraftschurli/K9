package com.blamejared.mcbot.mcp;

import com.blamejared.mcbot.mcp.ISrgMapping.MappingType;
import com.google.common.base.*;
import com.google.common.collect.*;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

public class SrgDatabase {
    
    private static final Pattern SRG_PATTERN = Pattern.compile("^(CL|FD|MD):\\s(.+)$");
    
    private final Table<MappingType, String, ISrgMapping> srgs = HashBasedTable.create();
    
    private final File zip;
    
    public SrgDatabase(String mcver) {
        this.zip = Paths.get(".", "data", mcver, "srgs", "mcp-" + mcver + "-srg.zip").toFile();
        try {
            reload();
        } catch (IOException e) {
            Throwables.propagate(e);
        }
    }

    public void reload() throws IOException {
        srgs.clear();
        ZipFile zipfile = new ZipFile(zip);
        List<String> srglines;
        try {
            srglines = IOUtils.readLines(zipfile.getInputStream(zipfile.getEntry("joined.srg")), Charsets.UTF_8);
        } finally {
            zipfile.close();
        }
        Matcher matcher = SRG_PATTERN.matcher("");
        SrgMappingFactory factory = new SrgMappingFactory();
        for (String srg : srglines) {
            matcher.reset(srg);
            if (matcher.matches()) {
                ISrgMapping mapping = factory.create(Arrays.stream(MappingType.values()).filter(t -> t.getSrgKey().equals(matcher.group(1))).findFirst().get(), matcher.group(2));
                if (!srgs.contains(mapping.getType(), mapping.getSRG())) {
                	srgs.put(mapping.getType(), mapping.getSRG(), mapping);
                }
            }
        }
    }

    public List<ISrgMapping> lookup(MappingType type, String name) {
        ISrgMapping ret = srgs.get(type, name);
        if (ret == null) {
            Predicate<Entry<String, ISrgMapping>> lookupFunc;
            if (type == MappingType.CLASS) {
                lookupFunc = e -> e.getKey().substring(e.getKey().lastIndexOf('/') + 1).equals(name);
            } else {
                lookupFunc = e -> e.getKey().contains(name);
            }
            List<ISrgMapping> found = srgs.row(type).entrySet().stream().filter(lookupFunc).map(Entry::getValue).collect(Collectors.toList());
            return found;
        }
        return Collections.singletonList(ret);
    }
}
