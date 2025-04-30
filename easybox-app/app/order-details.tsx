import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, View, StyleSheet, ScrollView, Image } from 'react-native';
import { Text, Card, Divider, Button } from 'react-native-paper';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';
import { useAuth } from '../lib/AuthContext';

type Order = {
    id: string;
    status: string;
    deliveryTime: string;
    easyboxAddress: string;
    qrCodeData?: string;
    compartmentId: number;
};

export default function OrderDetails() {
    const { id } = useLocalSearchParams();
    const { role } = useAuth(); // assumes: role = "bakery" | "client" | ...
    const [order, setOrder] = useState<Order | null>(null);

    useEffect(() => {
        api.get(`/orders/${id}`, {
            params: { role }
        })
            .then(res => setOrder(res.data))
            .catch(() => Alert.alert("Error", "Could not load order."));
    }, [id, role]);



    if (!order) return <Text style={styles.loading}>Loading...</Text>;

    const showQr = (
        (role === 'bakery' && order.status === 'waiting_bakery_drop_off') ||
        (role === 'client' && order.status === 'waiting_client_pick_up')
    );

    const reportIssue = (type: 'dirty' | 'broken') => {
        api.post(`/compartments/${order.compartmentId}/report-condition?issue=${type}`)
            .then(() => Alert.alert('Success', `Compartment marked as ${type}.`))
            .catch(() => Alert.alert('Error', 'Could not report the issue.'));
    };

    return (
        <View style={styles.container}>
            <ScreenHeader title="Order Details" showBack />
            <ScrollView contentContainerStyle={styles.content}>
                <Card style={styles.card}>
                    <Card.Title title="Order Summary" />
                    <Card.Content>
                        <Text style={styles.label}>Order ID:</Text>
                        <Text>{order.id}</Text>

                        <Text style={styles.label}>Status:</Text>
                        <Text>{formatStatus(order.status)}</Text>

                        <Text style={styles.label}>Delivery Time:</Text>
                        <Text>{formatDate(order.deliveryTime)}</Text>

                        <Text style={styles.label}>Easybox Location:</Text>
                        <Text>{order.easyboxAddress}</Text>

                        {showQr && order.qrCodeData && (
                            <>
                                <Divider style={styles.divider} />
                                <Text style={styles.label}>QR Code:</Text>
                                <Image
                                    source={{ uri: `data:image/png;base64,${order.qrCodeData}` }}
                                    style={styles.qr}
                                />
                            </>
                        )}

                        <Divider style={styles.divider} />
                        <Text style={styles.label}>Report Issue:</Text>
                        <View style={styles.issueRow}>
                            <Button
                                mode="outlined"
                                onPress={() => reportIssue('dirty')}
                                style={styles.issueBtn}
                            >
                                Compartment is Dirty
                            </Button>
                            <Button
                                mode="outlined"
                                onPress={() => reportIssue('broken')}
                                style={styles.issueBtn}
                            >
                                Compartment is Broken
                            </Button>
                        </View>
                    </Card.Content>
                </Card>
            </ScrollView>
        </View>
    );
}

function formatStatus(status: string) {
    return status.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleString();
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#fff' },
    content: { padding: 20 },
    card: { borderRadius: 10, elevation: 3 },
    label: { fontWeight: 'bold', marginTop: 12 },
    divider: { marginVertical: 12 },
    qr: { width: 200, height: 200, alignSelf: 'center', marginVertical: 10 },
    issueRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
    issueBtn: { flex: 1, marginHorizontal: 5 },
    loading: { marginTop: 50, textAlign: 'center' },
});
