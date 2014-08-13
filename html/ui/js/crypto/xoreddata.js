function xorProcess(data, position, length, myPrivateKey, theirPublicKey, nonce) {
    var i=0, j=0;
    var key;
    var seed = getSharedKey(myPrivateKey, theirPublicKey);
    for (i = 0; i < 32; i++) {
        seed[i] ^= nonce[i];
    }
    
    seed = SHA256_hash(seed, true);

    for (i = 0; i < ~~(length / 32); i++)  {
        key = SHA256_hash(seed, true);
        for (j = 0; j < 32; j++) {
            data[position++] ^= key[j];
            seed[j] = (~seed[j]) & 0xff;
        }
        seed = SHA256_hash(seed, true);
    }

    key = SHA256_hash(seed, true);

    for (i = 0; i < length % 32; i++) {
        data[position++] ^= key[i];
    }
}

function xorEncrypt(data, position, length, myPrivateKey, theirPublicKey) {
    var tmp = new Int8Array(32);
    var nonce = fromInt8Array(window.crypto.getRandomValues(tmp));
    
       // nonce = [145, 70, 101, 30, 97, 194, 148, 211, 182, 15, 200, 96, 76, 244, 45, 60, 53, 58, 91, 95, 146, 29, 79, 187, 81, 33, 213, 51, 174, 53, 55, 35];


    xorProcess(data, position, length, myPrivateKey, theirPublicKey, nonce);
    
    return nonce;   
}

function xorDecrypt(data, position, length, myPrivateKey, theirPublicKey, nonce) {
    xorProcess(data, position, length, myPrivateKey, theirPublicKey, nonce);
}

function XoredData() {
    this.data = [];
    this.nonce = [];
    
    this.init = function (data, nonce) {
        this.data = data;
        this.nonce = nonce;
        
        return this;
    };
    
    this.encrypt = function (plaintext, myPrivateKey, theirPublicKey) {
        var nonce = xorEncrypt(plaintext, 0, plaintext.length, myPrivateKey, theirPublicKey);
        
        this.init(plaintext, nonce);
        return this;
    };
    
    this.decrypt = function (myPrivateKey, theirPublicKey) {    	
        var ciphertext = this.data.slice(0);
        
        xorDecrypt(ciphertext, 0, ciphertext.length, myPrivateKey, theirPublicKey, this.nonce);
        return ciphertext;
    };
    
    this.getData = function () {
        return data;
    };
    
    this.getNonce = function () {
        return nonce;
    };
}


function getSharedKey(key1, key2) {
    return converters.shortArrayToByteArray(curve25519_(converters.byteArrayToShortArray(key1), converters.byteArrayToShortArray(key2), null));
}

function fromInt8Array(array) {
	var val;
	
	result = new Array(array.length);
	
	for (i=0; i<array.length; i++) {
		if (array[i] < 0) {
			result[i] = (array[i] + 256);
		} else {
			result[i] = array[i];
		}
	}
	
	return result;
}