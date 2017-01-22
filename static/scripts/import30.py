import os
import os.path
import sys
import logging
import traceback
import shutil
import json
import re

from ldif import LDIFParser, LDIFWriter
from jsonmerge import merge

# configure logging
logging.basicConfig(level=logging.DEBUG,
                    format='%(asctime)s %(levelname)-8s %(name)s %(message)s',
                    filename='import30.log',
                    filemode='w')
console = logging.StreamHandler()
console.setLevel(logging.INFO)
formatter = logging.Formatter('%(levelname)-8s %(message)s')
console.setFormatter(formatter)
logging.getLogger('').addHandler(console)
logging.getLogger('jsonmerge').setLevel(logging.WARNING)


class MyLDIF(LDIFParser):
    def __init__(self, input, output):
        LDIFParser.__init__(self, input)
        self.targetDN = None
        self.targetAttr = None
        self.targetEntry = None
        self.DNs = []
        self.lastDN = None
        self.lastEntry = None

    def getResults(self):
        return (self.targetDN, self.targetAttr)

    def getDNs(self):
        return self.DNs

    def getLastEntry(self):
        return self.lastEntry

    def handle(self, dn, entry):
        if self.targetDN is None:
            self.targetDN = dn
        self.lastDN = dn
        self.DNs.append(dn)
        self.lastEntry = entry
        if dn.lower().strip() == self.targetDN.lower().strip():
            self.targetEntry = entry
            if self.targetAttr in entry:
                self.targetAttr = entry[self.targetAttr]


class Migration(object):
    def __init__(self, backup):
        logging.info("Starting migration.")
        self.backupDir = backup
        self.ldifDir = os.path.join(backup, 'ldif')
        self.certsDir = os.path.join(backup, 'etc')
        self.currentDir = os.path.dirname(os.path.realpath(__file__))
        self.workingDir = os.path.join(self.currentDir, 'migration')
        self.os_types = ['centos', 'redhat', 'fedora', 'ubuntu', 'debian']
        self.os = self.detect_os_type()
        self.service = "/usr/sbin/service"
        if self.os is 'centos':
            self.service = "/sbin/service"

        self.slapdConf = "/opt/symas/etc/openldap/slapd.conf"
        self.slapcat = "/opt/symas/bin/slapcat"
        self.slapadd = "/opt/symas/bin/slapadd"
        self.keytool = "/opt/jre/bin/keytool"
        self.key_store = "/opt/jre/jre/lib/security/cacerts"
        self.etc_hostname = "/etc/hostname"

        self.ldapDataFile = "/opt/gluu/data/data.mdb"

        self.currentData = os.path.join(self.workingDir, 'current.ldif')
        self.o_gluu = os.path.join(self.workingDir, "o_gluu.ldif")
        self.processTempFile = os.path.join(self.workingDir, "temp.ldif")
        self.o_site = "/install/community-edition-setup/static/cache-refresh/o_site.ldif"
        self.attrs = 1000
        self.objclasses = 1000

    def readFile(self, inFilePath):
        inFilePathText = None

        try:
            f = open(inFilePath)
            inFilePathText = f.read()
            f.close
        except:
            logging.warning("Error reading %s", inFilePath)
            logging.debug(traceback.format_exc())

        return inFilePathText

    def walk_function(self, a, directory, files):
        for f in files:
            fn = "%s/%s" % (directory, f)
            targetFn = fn.replace(self.backupDir, '')
            if os.path.isdir(fn):
                if not os.path.exists(targetFn):
                    os.mkdir(targetFn)
            else:
                try:
                    logging.debug("copying %s", targetFn)
                    shutil.copyfile(fn, targetFn)
                except:
                    logging.error("Error copying %s", targetFn)

    def detect_os_type(self):
        distro_info = self.readFile('/etc/redhat-release')
        if distro_info is None:
            distro_info = self.readFile('/etc/os-release')
        if 'CentOS' in distro_info:
            return self.os_types[0]
        elif 'Red Hat' in distro_info:
            return self.os_types[1]
        elif 'Ubuntu' in distro_info:
            return self.os_types[3]
        elif 'Debian' in distro_info:
            return self.os_types[4]
        else:
            return self.choose_from_list(self.os_types, "Operating System")

    def verifyBackupData(self):
        if not os.path.exists(self.backupDir):
            logging.error("Backup folder %s doesn't exist! Quitting migration",
                          self.backupDir)
            sys.exit(1)
        if not os.path.exists(self.ldifDir):
            logging.error("Backup doesn't contain directory for LDIF data."
                          " Nothing to migrate. Quitting.")
            sys.exit(1)

    def setupWorkDirectory(self):
        if not os.path.exists(self.workingDir):
            os.mkdir(self.workingDir)
        else:
            # Clean the directory in case its already present
            shutil.rmtree(self.workingDir)
            os.mkdir(self.workingDir)

    def getOutput(self, args):
        try:
            logging.debug("Running command : %s" % " ".join(args))
            output = os.popen(" ".join(args)).read().strip()
            return output
        except:
            logging.error("Error running command : %s" % " ".join(args))
            logging.error(traceback.format_exc())
            sys.exit(1)

    def copyCertificates(self):
        logging.info("Copying the Certificates.")
        os.path.walk("%s/etc" % self.backupDir, self.walk_function, None)

        logging.info("Updating the CA Certs Keystore.")
        keys = ['httpd', 'idp-signing', 'idp-encryption', 'shibidp', 'asimba',
                'openldap']
        hostname = self.readFile(self.etc_hostname).strip()
        # import all the keys into the keystore
        for key in keys:
            alias = "{0}_{1}".format(hostname, key)
            filename = "/etc/certs/{0}.crt".format(key)
            if not os.path.isfile(filename):
                continue  # skip the non-existant certs

            logging.debug('Deleting new %s', alias)
            result = self.getOutput(
                [self.keytool, '-delete', '-alias', alias, '-keystore',
                 self.key_store, '-storepass', 'changeit', '-noprompt'])
            logging.error(result) if 'error' in result else logging.debug('Delete operation success.')

            logging.debug('Importing old %s', alias)
            result = self.getOutput(
                [self.keytool, '-import', '-trustcacerts', '-file', filename,
                 '-alias', alias, '-keystore', self.key_store, '-storepass',
                 'changeit', '-noprompt'])
            logging.error(result) if 'error' in result else logging.debug('Certificate import success.')

    def stopSolserver(self):
        logging.info("Stopping OpenLDAP Server.")
        stop_msg = self.getOutput([self.service, 'solserver', 'stop'])
        output = self.getOutput([self.service, 'solserver', 'status'])
        if "is not running" in output:
            return
        else:
            logging.error("Couldn't stop the OpenLDAP server.")
            logging.error(stop_msg)
            sys.exit(1)

    def startSolserver(self):
        logging.info("Starting OpenLDAP Server.")
        start_msg = self.getOutput([self.service, 'solserver', 'start'])
        output = self.getOutput([self.service, 'solserver', 'status'])
        if "is running" in output:
            return
        else:
            logging.error("Couldn't start the OpenLDAP server.")
            logging.error(start_msg)
            sys.exit(1)

    def exportInstallData(self):
        logging.info("Exporting LDAP data.")
        output = self.getOutput([self.slapcat, '-f', self.slapdConf,
                                 '-l', self.currentData])
        logging.debug(output)

    def convertSchema(self, f):
        infile = open(f, 'r')
        output = ""

        for line in infile:
            if re.match('^dn:', line) or re.match('^objectClass:', line) or \
                    re.match('^cn:', line):
                continue
            # empty lines and the comments are copied as such
            if re.match('^#', line) or re.match('^\s*$', line):
                pass
            elif re.match('^\s\s', line):  # change the space indendation to tabs
                line = re.sub('^\s\s', '\t', line)
            elif re.match('^\s', line):
                line = re.sub('^\s', '\t', line)
            # Change the keyword for attributetype
            elif re.match('^attributeTypes:\s', line, re.IGNORECASE):
                line = re.sub('^attributeTypes:', '\nattributetype', line, 1,
                              re.IGNORECASE)
                oid = 'oxAttribute:' + str(self.attrs+1)
                line = re.sub('\s[\d]+\s', ' '+oid+' ', line, 1, re.IGNORECASE)
                self.attrs += 1
            # Change the keyword for objectclass
            elif re.match('^objectClasses:\s', line, re.IGNORECASE):
                line = re.sub('^objectClasses:', '\nobjectclass', line, 1,
                              re.IGNORECASE)
                oid = 'oxObjectClass:' + str(self.objclasses+1)
                line = re.sub('ox-[\w]+-oid', oid, line, 1, re.IGNORECASE)
                self.objclasses += 1
            else:
                logging.warning("Skipping Line: {}".format(line.strip()))
                line = ""

            output += line

        infile.close()
        return output

    def updateUserSchema(self, infile, outfile):
        with open(infile, 'r') as olduser:
            with open(outfile, 'w') as newuser:
                for line in olduser:
                    if 'SUP top' in line:
                        line = line.replace('SUP top', 'SUP gluuPerson')
                    newuser.write(line)

    def copyCustomSchema(self):
        logging.info("Converting Schema files of custom attributes.")
        sloc = os.path.join(self.backupDir, 'opt', 'opendj', 'config', 'schema')
        schema_99 = os.path.join(sloc, '99-user.ldif')
        schema_100 = os.path.join(sloc, '100-user.ldif')

        new_user = os.path.join(self.workingDir, 'new_99.ldif')
        user_file = os.path.join(self.workingDir, 'user.schema')
        outfile = open(user_file, 'w')
        output = ""

        if os.path.isfile(schema_99):
            output = self.convertSchema(schema_100)
            self.updateUserSchema(schema_99, new_user)
            output = output + "\n" + self.convertSchema(new_user)
        else:
            # If there is no 99-user file, then the schema def is in 100-user
            self.updateUserSchema(schema_100, new_user)
            output = self.convertSchema(new_user)

        outfile.write(output)
        outfile.close()
        shutil.copyfile(user_file, '/opt/gluu/schema/openldap/user.schema')

    def getEntry(self, fn, dn):
        parser = MyLDIF(open(fn, 'rb'), sys.stdout)
        parser.targetDN = dn
        parser.parse()
        return parser.targetEntry

    def getDns(self, fn):
        parser = MyLDIF(open(fn, 'rb'), sys.stdout)
        parser.parse()
        return parser.DNs

    def getOldEntryMap(self):
        files = os.listdir(self.ldifDir)
        dnMap = {}

        # get the new admin DN
        admin_ldif = '/install/community-edition-setup/output/people.ldif'
        admin_dn = self.getDns(admin_ldif)[0]

        for fn in files:
            dnList = self.getDns(os.path.join(self.ldifDir, fn))
            for dn in dnList:
                # skip the entry of Admin DN and appliance data
                if (fn == 'people.ldif' and admin_dn in dn) or \
                        ('appliance' in fn):
                    continue
                dnMap[dn] = fn
        return dnMap

    def processBackupData(self):
        logging.info('Processing the LDIF data.')

        processed_fp = open(self.processTempFile, 'w')
        ldif_writer = LDIFWriter(processed_fp)

        currentDNs = self.getDns(self.currentData)
        old_dn_map = self.getOldEntryMap()

        ignoreList = ['objectClass', 'ou', 'oxAuthJwks', 'oxAuthConfWebKeys']
        multivalueAttrs = ['oxTrustEmail', 'oxTrustPhoneValue', 'oxTrustImsValue',
                           'oxTrustPhotos', 'oxTrustAddresses', 'oxTrustRole',
                           'oxTrustEntitlements', 'oxTrustx509Certificate']

        # Rewriting all the new DNs in the new installation to ldif file
        for dn in currentDNs:
            new_entry = self.getEntry(self.currentData, dn)
            if "o=site" in dn:
                continue  # skip all the o=site DNs
            elif dn not in old_dn_map.keys():
                #  Write to the file if there is no matching old DN data
                ldif_writer.unparse(dn, new_entry)
                continue

            old_entry = self.getEntry(os.path.join(self.ldifDir, old_dn_map[dn]), dn)
            for attr in old_entry.keys():
                if attr in ignoreList:
                    continue

                if attr not in new_entry:
                    new_entry[attr] = old_entry[attr]
                elif old_entry[attr] != new_entry[attr]:
                    if len(old_entry[attr]) == 1:
                        try:
                            old_json = json.loads(old_entry[attr][0])
                            new_json = json.loads(new_entry[attr][0])
                            new_json = merge(new_json, old_json)
                            new_entry[attr] = [json.dumps(new_json)]
                        except:
                            new_entry[attr] = old_entry[attr]
                            logging.debug("Keeping old value for %s", attr)
                    else:
                        new_entry[attr] = old_entry[attr]
                        logging.debug("Keep multiple old values for %s", attr)
            ldif_writer.unparse(dn, new_entry)

        # Pick all the left out DNs from the old DN map and write them to the LDIF
        for dn in sorted(old_dn_map, key=len):
            if dn in currentDNs:
                continue  # Already processed

            entry = self.getEntry(os.path.join(self.ldifDir, old_dn_map[dn]), dn)

            for attr in entry.keys():
                if attr not in multivalueAttrs:
                    continue  # skip conversion

                attr_values = []
                for val in entry[attr]:
                    json_value = None
                    try:
                        json_value = json.loads(val)
                        if type(json_value) is list:
                            attr_values.extend([json.dumps(v) for v in json_value])
                    except:
                        logging.debug('Cannot parse multival %s in DN %s', attr, dn)
                        attr_values.append(val)
                entry[attr] = attr_values

            ldif_writer.unparse(dn, entry)

        # Finally
        processed_fp.close()

        # Update the Schema change for lastModifiedTime
        with open(self.processTempFile, 'r') as infile:
            with open(self.o_gluu, 'w') as outfile:
                for line in infile:
                    line.replace("lastModifiedTime", "oxLastAccessTime")
                    line.replace("cn=directory manager", "cn=directory manager,o=gluu")
                    outfile.write(line)

    def importProcessedData(self):
        logging.info("Importing Processed LDAP data.")
        count = len(os.listdir('/opt/gluu/data/'))
        backupfile = self.ldapDataFile + ".bkp_{0:02d}".format(count)
        logging.debug("Moving %s to %s.", self.ldapDataFile, backupfile)
        shutil.move(self.ldapDataFile, backupfile)

        output = self.getOutput([self.slapadd, '-c', '-b', 'o=gluu', '-f',
                                self.slapdConf, '-l', self.o_gluu])
        logging.debug(output)
        output = self.getOutput([self.slapadd, '-c', '-b', 'o=site', '-f',
                                self.slapdConf, '-l', self.o_site])
        logging.debug(output)

    def migrate(self):
        """Main function for the migration of backup data
        """
        self.verifyBackupData()
        self.copyCertificates()
        self.setupWorkDirectory()
        self.stopSolserver()
        self.copyCustomSchema()
        self.exportInstallData()
        self.processBackupData()
        self.importProcessedData()
        self.startSolserver()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "Usage: ./import30.py <path_to_backup_folder>"
        print "Example:\n ./import30.py /root/backup_24"
    else:
        migrator = Migration(sys.argv[1])
        migrator.migrate()
