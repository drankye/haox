package org.haox.kerb.ccache;

import org.haox.kerb.KrbInputStream;
import org.haox.kerb.spec.type.KerberosTime;
import org.haox.kerb.spec.type.common.*;
import org.haox.kerb.spec.type.ticket.Ticket;
import org.haox.kerb.spec.type.ticket.TicketFlags;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CredCacheInputStream extends KrbInputStream
{
    public CredCacheInputStream(InputStream in) {
        super(in);
    }

    @Override
    public PrincipalName readPrincipal(int version) throws IOException {
        NameType nameType = NameType.NT_UNKNOWN;
        if (version != CredentialCache.FCC_FVNO_1) {
            int typeValue = readInt();
            nameType = NameType.fromValue(typeValue);
        }

        int numComponents = readInt();
        if (version == CredentialCache.FCC_FVNO_1) {
            numComponents -= 1;
        }

        String realm = readCountedString();

        List<String> nameStrings = new ArrayList<String>();
        String component;
        for (int i = 0; i < numComponents; i++) { // sub 1 if version 0x501
            component = readCountedString();
            nameStrings.add(component);
        }

        PrincipalName principal = new PrincipalName(nameStrings, nameType);
        principal.setRealm(realm);

        return principal;
    }

    public EncryptionKey readKey(int version) throws IOException {
        if (version == CredentialCache.FCC_FVNO_3) {
            readShort(); //  ignore keytype
        }

        return super.readKey(version);
    }

    public KerberosTime[] readTimes() throws IOException {
        KerberosTime[] times = new KerberosTime[4];

        for (int i = 0; i < times.length; ++i) {
            times[i] = readTime();
        }

        return times;
    }

    public boolean readIsSkey() throws IOException {
        int value = readByte();
        return value == 1 ? true : false;
    }

    public HostAddresses readAddr() throws IOException {
        int numAddresses = readInt();
        if (numAddresses <= 0) {
            return null;
        }

        HostAddress[] addresses = new HostAddress[numAddresses];
        for (int i = 0; i < numAddresses; i++) {
            addresses[i] = readAddress();
        }

        HostAddresses result = new HostAddresses();
        result.addElements(addresses);
        return result;
    }

    public HostAddress readAddress() throws IOException {
        int typeValue = readShort();
        HostAddrType addrType = HostAddrType.fromValue(typeValue);
        byte[] addrData = readCountedOctets();

        HostAddress addr = new HostAddress();
        addr.setAddrType(addrType);
        addr.setAddress(addrData);

        return addr;
    }

    public AuthorizationData readAuthzData() throws IOException {
        int numEntries = readInt();
        if (numEntries <= 0) {
            return null;
        }

        AuthorizationDataEntry[] authzData = new AuthorizationDataEntry[numEntries];
        for (int i = 0; i < numEntries; i++) {
            authzData[i] = readAuthzDataEntry();
        }

        AuthorizationData result = new AuthorizationData();
        result.addElements(authzData);
        return result;
    }

    public AuthorizationDataEntry readAuthzDataEntry() throws IOException {
        int typeValue = readShort();
        AuthorizationType authzType = AuthorizationType.fromValue(typeValue);
        byte[] authzData = readCountedOctets();

        AuthorizationDataEntry authzEntry = new AuthorizationDataEntry();
        authzEntry.setAuthzType(authzType);
        authzEntry.setAuthzData(authzData);

        return authzEntry;
    }

    @Override
    public int readOctetsCount() throws IOException {
        return readInt();
    }

    public TicketFlags readTicketFlags() throws IOException {
        int flags = readInt();
        TicketFlags tktFlags = new TicketFlags(flags);
        return tktFlags;
    }

    public Ticket readTicket() throws IOException {
        byte[] ticketData = readCountedOctets();
        if (ticketData == null) {
            return null;
        }

        Ticket ticket = new Ticket();
        ticket.decode(ticketData);
        return ticket;
    }
}