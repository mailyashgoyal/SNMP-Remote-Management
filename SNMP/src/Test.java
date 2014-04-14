import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;


public class Test {

	public static void main(String args[])
	{
		SnmpTest st = new SnmpTest();
		st.snmpGet("192.168.0.14","public","1.3.6.1.2.1.25.1.6.0");//.2.3.1.3");//.6.1.2.1.25.2.3.1.5");//2.1.1.1.0");
	//	st.snmpSet("127.0.0.1","public","1.3.6.1.2.1.1.5.0",123);
	}

}
