package org.example.duanparking.common.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.example.duanparking.common.dto.ParkingSlotDTO;
import org.example.duanparking.server.dao.ParkingSlotEntity;

public interface ParkingInterface extends Remote {
   /*
   * Client gọi server sẽ trả lời
   */

   List<ParkingSlotDTO> getAllSlots() throws RemoteException;
   void updateSlotStatus(String spotId, String status,String plateName, String owner, String arriveTime, String brand, String infor) throws RemoteException;
   ParkingSlotEntity getUpdatedSlot(String spotId) throws RemoteException;
   void registerClient(ClientCallback client) throws RemoteException; // Đăng ký client

   boolean checkId(String plateName) throws RemoteException;
   boolean checkPlace (String placename) throws RemoteException;
   void ping() throws RemoteException;
   void syncSlots(List<ParkingSlotDTO> slots) throws RemoteException;
   void onSlotUpdated(ParkingSlotDTO slot) throws RemoteException;
}
