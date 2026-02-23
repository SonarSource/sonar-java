The plugin JAR contains a `licenses/` folder which contains the licenses of all the used libraries.
These licenses have to be txt files. However, some libraries ship html files, in which case they need to be overwritten.

The files which overwrite the html file are located in this folder. The overwriting itself is configured
in the `pom.xml` of the plugin module.
