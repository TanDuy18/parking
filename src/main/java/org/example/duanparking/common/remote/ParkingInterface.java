package org.example.duanparking.common.remote;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.example.duanparking.common.dto.*;

public interface ParkingInterface extends Remote {
   /*
   * Client gọi server sẽ trả lời
   */

   List<ParkingSlotDTO> getAllSlots() throws RemoteException;
   int updateSlotStatus(ParkingSlotDTO slot) throws RemoteException;
   void registerClient(ClientCallback client) throws RemoteException; // Đăng ký client
   void unregisterClient(ClientCallback client) throws RemoteException;
   void takeVehicleInFromSync(ParkingInEvent slot) throws RemoteException;
   void takeVehicleOutFromSync(ParkingOutEvent slot) throws RemoteException;
   boolean checkIdIn(String plateName) throws RemoteException;
   boolean checkPlaceIn (String placename) throws RemoteException;
   ParkingSlotDTO getVehicleInfoForOut(String plateNumber) throws RemoteException;
   ParkingSlotDTO getVehicleInfoForIn(String plateNumber) throws RemoteException;
   boolean takeVehicleOut(ParkingSlotDTO slot) throws RemoteException;
   void rentPlace(RentDTO place) throws RemoteException;
   double getHourlyRate(String place, String vehicleType) throws RemoteException;
   void ping() throws RemoteException;
}
